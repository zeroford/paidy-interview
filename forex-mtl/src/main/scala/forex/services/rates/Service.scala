package forex.services.rates

import scala.concurrent.duration._
import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.all._
import forex.clients.OneFrameClient
import forex.clients.oneframe.Protocol.OneFrameRatesResponse
import forex.domain.cache.FetchStrategy
import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.{ PivotRate, Rate, Timestamp }
import forex.services.{ CacheService, PivotPair }
import forex.services.rates.concurrent.BucketLocks
import forex.services.rates.{ errors => Error }
import org.typelevel.log4cats.Logger

import java.time.Instant

final class Service[F[_]: Concurrent: Logger](
    oneFrameClient: OneFrameClient[F],
    cache: CacheService[F],
    locks: BucketLocks[F],
    ttl: FiniteDuration
) extends Algebra[F] {

  private val Pivot = Currency.USD

  private def cacheKey(currency: Currency): String = s"$Pivot$currency"

  override def get(pair: Rate.Pair, now: Instant): F[AppError Either Rate] =
    getRateOrFetch(pair, now).flatMap {
      case Right((base, quote)) =>
        val rate = Rate.fromPivotRate(base, quote)
        Logger[F].debug(
          "[RatesService] Calculate price from pivot " +
            s"${base.currency}(${base.price.value}) -> ${quote.currency}(${quote.price.value})" +
            s" => price: ${rate.price.value}"
        ) >> rate.asRight[AppError].pure[F]
      case Left(err) => err.asLeft[Rate].pure[F]
    }

  private def getRateOrFetch(pair: Rate.Pair, now: Instant): F[AppError Either PivotPair] = {
    val action =
      (getFromCachePivot(pair.from, now), getFromCachePivot(pair.to, now)).tupled.flatMap {
        case (Right(Some(base)), Right(Some(quote))) => (base -> quote).asRight[AppError].pure[F]
        case (Right(baseOpt), Right(quoteOpt))       => fetchWithStrategy(pair, baseOpt, quoteOpt)
        case (baseRes, quoteRes)                     => Error.combine(baseRes, quoteRes).asLeft[PivotPair].pure[F]
      }
    FetchStrategy.fromPair(pair) match {
      case FetchStrategy.All => locks.withBuckets(action)
      case other             => locks.withBucket(BucketLocks.bucketFor(other))(action)
    }
  }

  private def getFromCachePivot(currency: Currency, now: Instant): F[AppError Either Option[PivotRate]] =
    if (currency == Pivot) PivotRate.default(Pivot, now).some.asRight[AppError].pure[F]
    else {
      cache
        .get[String, PivotRate](cacheKey(currency))
        .flatMap {
          case Right(prOpt) => prOpt.filter(pr => Timestamp.withinTtl(pr.timestamp, now, ttl)).asRight[AppError].pure[F]
          case Left(err)    => err.asLeft[Option[PivotRate]].pure[F]
        }
    }

  private def fetchWithStrategy(
      pair: Rate.Pair,
      baseOpt: Option[PivotRate],
      quoteOpt: Option[PivotRate]
  ): F[AppError Either PivotPair] = {
    val strategy     = FetchStrategy.fromPair(pair, baseOpt.isEmpty, quoteOpt.isEmpty)
    val requestPairs = strategy match {
      case FetchStrategy.MostUsed  => Currency.mostUsedCurrencies.map(Rate.Pair(Pivot, _))
      case FetchStrategy.LeastUsed => Currency.otherCurrencies.map(Rate.Pair(Pivot, _))
      case FetchStrategy.All       => Currency.allCurrencies.map(Rate.Pair(Pivot, _))
    }

    (for {
      response <- EitherT(oneFrameClient.getRates(requestPairs))
      pivotPair <- EitherT.fromEither[F](extractPivotRates(response, pair, baseOpt, quoteOpt))
      _ <- EitherT(cacheRates(response))
    } yield pivotPair).value
  }

  private def cacheRates(response: OneFrameRatesResponse): F[AppError Either Unit] =
    response.traverse { r =>
      Currency.fromString(r.to) match {
        case Right(currency) =>
          val pivotRate = PivotRate.fromResponse(currency, r.price, r.time_stamp)
          cache.put(cacheKey(currency), pivotRate)
        case Left(_) => ().asRight[AppError].pure[F]
      }
    } flatMap { results =>
      results.collectFirst { case Left(e) => e } match {
        case Some(e) =>
          e.asLeft[Unit].pure[F]
        case None =>
          ().asRight[AppError].pure[F]
      }
    }

  private def extractPivotRates(
      response: OneFrameRatesResponse,
      pair: Rate.Pair,
      baseOpt: Option[PivotRate],
      quoteOpt: Option[PivotRate]
  ): AppError Either PivotPair = {
    def lookup(cur: Currency): Option[PivotRate] =
      response.collectFirst {
        case r if r.from == Pivot.toString && r.to == cur.toString =>
          PivotRate.fromResponse(cur, r.price, r.time_stamp)
      }

    def pick(cur: Currency, cached: Option[PivotRate]): Option[PivotRate] = cached.orElse(lookup(cur))

    (pick(pair.from, baseOpt), pick(pair.to, quoteOpt)) match {
      case (Some(base), Some(quote)) => Right(base -> quote)
      case _                         => Left(Error.notFound(pair))
    }
  }

}

object Service {
  def apply[F[_]: Concurrent: Logger](
      oneFrameClient: OneFrameClient[F],
      cache: CacheService[F],
      locks: BucketLocks[F],
      ttl: FiniteDuration
  ): Algebra[F] = new Service[F](oneFrameClient, cache, locks, ttl)
}
