package forex.services.rates

import java.time.Instant
import scala.concurrent.duration._

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.all._
import org.typelevel.log4cats.Logger

import forex.clients.OneFrameClient
import forex.domain.cache.FetchStrategy
import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.{ PivotRate, Rate, Timestamp }
import forex.services.{ CacheService, PivotPair }
import forex.services.rates.concurrent.BucketLocks
import forex.services.rates.{ errors => Error }

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

  private def cacheRates(response: List[PivotRate]): F[AppError Either Unit] =
    response.traverse_(r => EitherT(cache.put(cacheKey(r.currency), r))).value.flatTap {
      case Right(_)  => Logger[F].debug(s"[RatesService] Cached ${response.size} pivot rates")
      case Left(err) => Logger[F].error(s"[RatesService] Failed to cache pivot rates: ${err.getMessage}")
    }

  private def extractPivotRates(
      response: List[PivotRate],
      pair: Rate.Pair,
      baseOpt: Option[PivotRate],
      quoteOpt: Option[PivotRate]
  ): AppError Either PivotPair = {
    lazy val idx: Map[Currency, PivotRate] =
      response.iterator.map(r => r.currency -> r).toMap

    def pick(currency: Currency, cached: Option[PivotRate]): Option[PivotRate] =
      cached.orElse(idx.get(currency))

    (for {
      base <- pick(pair.from, baseOpt)
      quote <- pick(pair.to, quoteOpt)
    } yield (base, quote)).toRight(Error.notFound(pair))
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
