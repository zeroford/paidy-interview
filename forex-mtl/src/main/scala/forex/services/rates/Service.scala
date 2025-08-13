package forex.services.rates

import cats.effect.Concurrent
import cats.syntax.all._
import forex.domain.cache.{ FetchStrategy, PivotRate }
import forex.domain.currency.Currency
import forex.domain.rates.{ Rate, Timestamp }
import forex.clients.OneFrameClient
import forex.clients.oneframe.Protocol.OneFrameRatesResponse
import forex.domain.error.AppError
import forex.services.PivotPair
import forex.services.cache.{ Algebra => CacheAlgebra }
import org.typelevel.log4cats.Logger

import scala.concurrent.duration._

class Service[F[_]: Concurrent: Logger](oneFrameClient: OneFrameClient[F], cache: CacheAlgebra[F], ttl: FiniteDuration)
    extends Algebra[F] {

  private val Pivot = Currency.USD

  private def cacheKey(currency: Currency): String = s"$Pivot$currency"

  override def get(pair: Rate.Pair): F[AppError Either Rate] =
    getRateOrFetch(pair).flatMap {
      case Right((base, quote)) =>
        val rate = Rate.fromPivotRate(base, quote)
        Logger[F].debug(
          "[RatesService] Calculate price from pivot " +
            s"${base.currency}(${base.price.value}) -> ${quote.currency}(${base.price.value})" +
            s" => price: ${rate.price.value}"
        ) >> rate.asRight[AppError].pure[F]
      case Left(err) => err.asLeft[Rate].pure[F]
    }

  private def getRateOrFetch(pair: Rate.Pair): F[AppError Either PivotPair] =
    (getFromCachePivot(pair.from), getFromCachePivot(pair.to)).tupled.flatMap {
      case (Right(Some(base)), Right(Some(quote))) =>
        (base -> quote).asRight[AppError].pure[F]
      case (Right(baseOpt), Right(quoteOpt)) =>
        fetchWithStrategy(pair, baseOpt, quoteOpt)
      case (Left(error), _) => error.asLeft[PivotPair].pure[F]
      case (_, Left(error)) => error.asLeft[PivotPair].pure[F]
    }

  private def getFromCachePivot(currency: Currency): F[AppError Either Option[PivotRate]] =
    if (currency == Pivot) PivotRate.default(Pivot).some.asRight[AppError].pure[F]
    else
      cache
        .get[String, PivotRate](cacheKey(currency))
        .map(_.map {
          case Some(pr) if Timestamp.isWithinTTL(pr.timestamp, ttl) => Some(pr)
          case _                                                    => None
        })

  private def fetchWithStrategy(
      pair: Rate.Pair,
      baseOpt: Option[PivotRate],
      quoteOpt: Option[PivotRate]
  ): F[AppError Either PivotPair] = {
    val strategy     = FetchStrategy.fromPair(pair, baseOpt.isEmpty, quoteOpt.isEmpty)
    val requestPairs = strategy match {
      case FetchStrategy.MostUsed  => Currency.mostUsedCurrencies.map(Rate.Pair(Pivot, _))
      case FetchStrategy.LeastUsed => Currency.leastUsedCurrencies.map(Rate.Pair(Pivot, _))
      case FetchStrategy.All       => Currency.allCurrencies.map(Rate.Pair(Pivot, _))
    }
    oneFrameClient.getRates(requestPairs).flatMap {
      case Left(err: AppError) => err.asLeft[PivotPair].pure[F]
      case Right(response)     =>
        for {
          pivotPair <- extractPivotRates(response, pair, baseOpt, quoteOpt).map(_.asRight[AppError])
          _ <- cacheRates(response).map(_ => ())
        } yield pivotPair
    }
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
  ): F[(PivotRate, PivotRate)] = {
    def lookup(cur: Currency): Option[PivotRate] =
      response.collectFirst {
        case r if r.from == Pivot.toString && r.to == cur.toString =>
          PivotRate.fromResponse(cur, r.price, r.time_stamp)
      }

    def pick(cur: Currency, cached: Option[PivotRate]): PivotRate =
      cached.orElse(lookup(cur)).getOrElse(PivotRate.default(cur))

    (pick(pair.from, baseOpt) -> pick(pair.to, quoteOpt)).pure[F]
  }

}

object Service {
  def apply[F[_]: Concurrent: Logger](
      oneFrameClient: OneFrameClient[F],
      cache: CacheAlgebra[F],
      ttl: FiniteDuration
  ): Algebra[F] = new Service[F](oneFrameClient, cache, ttl)
}
