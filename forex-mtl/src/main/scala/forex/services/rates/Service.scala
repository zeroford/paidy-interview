package forex.services.rates

import java.time.OffsetDateTime
import cats.effect.Concurrent
import cats.syntax.all._
import forex.domain.cache.PivotRate
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.integrations.OneFrameClient
import forex.integrations.oneframe.Protocol.GetRateResponse
import forex.services.cache.{ Algebra => CacheAlgebra }
import forex.services.rates.errors.RatesServiceError

import scala.concurrent.duration._

class Service[F[_]: Concurrent](oneFrameClient: OneFrameClient[F], cache: CacheAlgebra[F], ttl: FiniteDuration)
    extends Algebra[F] {

  private val Pivot = Currency.USD

  private def cacheKey(pair: Rate.Pair): String = s"${pair.from}${pair.to}"

  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    getFromCache(pair).flatMap {
      case Some(rate) => rate.asRight[RatesServiceError].pure[F]
      case None       =>
        val pivotBase  = Rate.Pair(Pivot, pair.from)
        val pivotQuote = Rate.Pair(Pivot, pair.to)
        getRateOrFetch(pivotBase, pivotQuote).attempt.flatMap {
          case Right((baseRate, quoteRate)) =>
            val calculatedRate = Rate.fromPivotRate(baseRate, quoteRate)
            cache.put[String, Rate](cacheKey(pair), rate) *> rate.asRight[RatesServiceError].pure[F]
          case Left(err) =>
            (RatesServiceError.OneFrameLookupFailed(
              Option(err.getMessage).getOrElse("Unknown error")
            ): RatesServiceError).asLeft[Rate].pure[F]
        }
    }

  private def getRateOrFetch(
      pivotBase: Rate.Pair,
      pivotQuote: Rate.Pair
  ): F[(PivotRate, PivotRate)] =
    (getFromCachePivot(pivotBase), getFromCachePivot(pivotQuote)).tupled.flatMap {
      case (Some(base), Some(quote)) => (base -> quote).pure[F]
      case _                         => fetchBasedOnUsage(pivotBase, pivotQuote)
    }

  private def getFromCachePivot(pair: Rate.Pair): F[Option[PivotRate]] =
    cache.get[String, PivotRate](cacheKey(pair)).map {
      case Some(pr) if Timestamp.isWithinTTL(pr.timestamp, ttl) => Some(pr)
      case _                                                    => None
    }

  private def getFromCache(pair: Rate.Pair): F[Option[Rate]] =
    cache.get[String, Rate](cacheKey(pair)).map {
      case Some(rate) if Timestamp.isWithinTTL(rate.timestamp, ttl) => Some(rate)
      case _                                                        => None
    }

  private def fetchBasedOnUsage(
      pivotBase: Rate.Pair,
      pivotQuote: Rate.Pair
  ): F[(PivotRate, PivotRate)] = {
    val requiredPairs = List(pivotBase, pivotQuote).filterNot(pair => pair.from == pair.to)

    fetchAndExtract(requiredPairs, pivotBase, pivotQuote)
  }

  private def fetchAndExtract(
      pivots: List[Rate.Pair],
      pivotBase: Rate.Pair,
      pivotQuote: Rate.Pair
  ): F[(PivotRate, PivotRate)] =
    for {
      response <- oneFrameClient.getRates(pivots)
      pair <- response match {
                case Right(res) =>
                  cacheRates(res) *>
                    extractPivotRates(res, pivotBase, pivotQuote)
                case Left(err) =>
                  RatesServiceError
                    .OneFrameLookupFailed(Option(err.toString).getOrElse("Unknown OneFrame error"))
                    .raiseError[F, (PivotRate, PivotRate)]
              }
    } yield pair

  private def cacheRates(response: GetRateResponse): F[Unit] =
    response.rates.traverse_ { r =>
      val fromCurrency = Currency.fromString(r.from).getOrElse(Currency.USD)
      val toCurrency   = Currency.fromString(r.to).getOrElse(Currency.USD)
      val pair         = Rate.Pair(fromCurrency, toCurrency)
      val price        = Price(r.price)
      val timestamp    = Timestamp(OffsetDateTime.parse(r.time_stamp))
      cache.put[String, PivotRate](cacheKey(pair), PivotRate(toCurrency, price, timestamp))
    }

  private def extractPivotRates(
      rates: GetRateResponse,
      pivotBase: Rate.Pair,
      pivotQuote: Rate.Pair
  ): F[(PivotRate, PivotRate)] = {

    def find(pair: Rate.Pair): Option[PivotRate] =
      rates.rates
        .find { rate =>
          val fromCurrency = Currency.fromString(rate.from).getOrElse(Currency.USD)
          val toCurrency   = Currency.fromString(rate.to).getOrElse(Currency.USD)
          fromCurrency == pair.from && toCurrency == pair.to
        }
        .map { rate =>
          val currency  = Currency.fromString(rate.to).getOrElse(Currency.USD)
          val price     = Price(rate.price)
          val timestamp = Timestamp(OffsetDateTime.parse(rate.time_stamp))
          PivotRate(currency, price, timestamp)
        }

    (find(pivotBase), find(pivotQuote)) match {
      case (Some(base), Some(quote)) =>
        (base -> quote).pure[F]

      case (None, Some(quote)) =>
        // If base is missing, try to find USD/USD rate from OneFrame response
        val usdUsdRate = rates.rates
          .find { rate =>
            rate.from == "USD" && rate.to == "USD"
          }
          .map { rate =>
            PivotRate(
              currency = Currency.USD,
              price = Price(rate.price),
              timestamp = Timestamp(java.time.OffsetDateTime.parse(rate.time_stamp))
            )
          }
          .getOrElse {
            // Fallback to price = 1.0 if USD/USD not found
            PivotRate(
              currency = Currency.USD,
              price = Price(BigDecimal(1.0)),
              timestamp = quote.timestamp
            )
          }
        (usdUsdRate -> quote).pure[F]

      case (Some(base), None) =>
        // If quote is missing, try to find USD/USD rate from OneFrame response
        val usdUsdRate = rates.rates
          .find { rate =>
            rate.from == "USD" && rate.to == "USD"
          }
          .map { rate =>
            PivotRate(
              currency = Currency.USD,
              price = Price(rate.price),
              timestamp = Timestamp(java.time.OffsetDateTime.parse(rate.time_stamp))
            )
          }
          .getOrElse {
              price = Price(BigDecimal(1.0)),
              timestamp = base.timestamp
            )
          }
        (base -> usdUsdRate).pure[F]
        val usdUsdRate = rates.rates
          .find { rate =>
          .map { rate =>
            PivotRate(
              currency = Currency.USD,
              price = Price(rate.price),
              timestamp = Timestamp(java.time.OffsetDateTime.parse(rate.time_stamp))
            )
          }
          .getOrElse {
            val now = Timestamp(java.time.OffsetDateTime.now())
            PivotRate(
              currency = Currency.USD,
              price = Price(BigDecimal(1.0)),
              timestamp = now
            )
          }
        (usdUsdRate -> usdUsdRate).pure[F]
    }
  }

}

object Service {
  def apply[F[_]: Concurrent](
      oneFrameClient: OneFrameClient[F],
      cache: CacheAlgebra[F],
      ttl: FiniteDuration
  ): Algebra[F] = new Service[F](oneFrameClient, cache, ttl)
}
