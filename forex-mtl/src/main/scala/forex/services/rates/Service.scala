package forex.services.rates

import cats.effect.Concurrent
import cats.syntax.all._
import forex.domain.cache.{ FetchStrategy, PivotRate }
import forex.domain.currency.Currency
import forex.domain.rates.{ Rate, Timestamp }
import forex.integrations.OneFrameClient
import forex.integrations.oneframe.Protocol.GetRateResponse
import forex.services.PivotPair
import forex.services.cache.{ Algebra => CacheAlgebra }
import forex.services.rates.errors.RatesServiceError

import scala.concurrent.duration._

class Service[F[_]: Concurrent](oneFrameClient: OneFrameClient[F], cache: CacheAlgebra[F], ttl: FiniteDuration)
    extends Algebra[F] {

  private val Pivot = Currency.USD

  private def cacheKey(currency: Currency): String = s"${Pivot}${currency}"

  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    cache.get[String, Rate](s"${pair.from}${pair.to}").flatMap {
      case Some(rate) if Timestamp.isWithinTTL(rate.timestamp, ttl) =>
        rate.asRight[RatesServiceError].pure[F]
      case _ =>
        getRateOrFetch(pair).flatMap {
          case Right((base, quote)) =>
            val rate = Rate.fromPivotRate(base, quote)
            cache.put(s"${pair.from}${pair.to}", rate).map(_ => rate.asRight[RatesServiceError])
          case Left(err) =>
            err.asLeft[Rate].pure[F]
        }
    }

  private def getRateOrFetch(pair: Rate.Pair): F[RatesServiceError Either PivotPair] =
    (getFromCachePivot(pair.from), getFromCachePivot(pair.to)).tupled.flatMap {
      case (Some(base), Some(quote)) =>
        println(
          s"DEBUG: getRateOrFetch - using cached rates: ${base.currency}(${base.price.value}) -> ${quote.currency}(${quote.price.value})"
        )
        (base -> quote).asRight[RatesServiceError].pure[F]
      case (baseOpt, quoteOpt) =>
        fetchWithStrategy(pair, baseOpt, quoteOpt)
    }

  private def getFromCachePivot(currency: Currency): F[Option[PivotRate]] =
    if (currency == Pivot) PivotRate.default(Pivot).some.pure[F]
    else
      cache.get[String, PivotRate](cacheKey(currency)).map {
        case Some(pr) if Timestamp.isWithinTTL(pr.timestamp, ttl) => Some(pr)
        case _                                                    => None
      }

  private def fetchWithStrategy(
      pair: Rate.Pair,
      baseOpt: Option[PivotRate],
      quoteOpt: Option[PivotRate]
  ): F[RatesServiceError Either PivotPair] = {
    val strategy     = determineStrategy(pair, baseOpt.isEmpty, quoteOpt.isEmpty)
    val requestPairs = strategy match {
      case FetchStrategy.MostUsed => Currency.mostUsedCurrencies.map(Rate.Pair(Pivot, _))
      case FetchStrategy.All      => Currency.allCurrencies.map(Rate.Pair(Pivot, _))
    }

    for {
      responseEither <- oneFrameClient.getRates(requestPairs)

      result <- responseEither.fold(
                  err => RatesServiceError.OneFrameLookupFailed(err.getMessage).asLeft[PivotPair].pure[F],
                  response =>
                    for {
                      pivotRates <- extractPivotRates(response, pair, baseOpt, quoteOpt)
                      _ <- cacheRates(response)
                    } yield pivotRates.asRight[RatesServiceError]
                )
    } yield result
  }

  private def determineStrategy(pair: Rate.Pair, needBase: Boolean, needQuote: Boolean): FetchStrategy =
    if (needBase && !Currency.isMostUsed(pair.from)) { FetchStrategy.All }
    else if (needQuote && !Currency.isMostUsed(pair.to)) { FetchStrategy.All }
    else { FetchStrategy.MostUsed }

  private def cacheRates(response: GetRateResponse): F[Unit] =
    response.rates.traverse { r =>
      Currency.fromString(r.to) match {
        case Right(currency) =>
          val pivotRate = PivotRate.fromResponse(currency, r.price, r.time_stamp)
          cache.put(cacheKey(currency), pivotRate)
        case Left(_) =>
          ().pure[F]
      }
    }.void

  private def extractPivotRates(
      response: GetRateResponse,
      pair: Rate.Pair,
      baseOpt: Option[PivotRate],
      quoteOpt: Option[PivotRate]
  ): F[(PivotRate, PivotRate)] = {
    def lookup(cur: Currency): Option[PivotRate] =
      response.rates.collectFirst {
        case r if r.from == Pivot.toString && r.to == cur.toString =>
          PivotRate.fromResponse(cur, r.price, r.time_stamp)
      }

    def pick(cur: Currency, cached: Option[PivotRate]): PivotRate =
      cached.orElse(lookup(cur)).getOrElse(PivotRate.default(cur))

    (pick(pair.from, baseOpt) -> pick(pair.to, quoteOpt)).pure[F]
  }

}

object Service {
  def apply[F[_]: Concurrent](
      oneFrameClient: OneFrameClient[F],
      cache: CacheAlgebra[F],
      ttl: FiniteDuration
  ): Algebra[F] = new Service[F](oneFrameClient, cache, ttl)
}
