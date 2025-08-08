package forex.services.rates

import cats.effect.Concurrent
import cats.syntax.all._
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.integrations.OneFrameClient
import forex.services.cache.{ Algebra => CacheAlgebra }
import forex.services.rates.errors.RatesServiceError
import scala.concurrent.duration._

class Service[F[_]: Concurrent](
    oneFrameClient: OneFrameClient[F],
    cache: CacheAlgebra[F],
    ttl: FiniteDuration
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    if (pair.from == pair.to) {
      Rate(pair, Price(1.0), Timestamp.now)
        .asRight[RatesServiceError]
        .pure[F]
    } else {
      cache.get[String, Rate](key(pair)).flatMap {
        case Some(rate) if isValid(rate.timestamp) =>
          rate.asRight[RatesServiceError].pure[F]
        case _ =>
          fetchAndCacheRate(pair)
      }
    }

  private def fetchAndCacheRate(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    oneFrameClient.getRate(pair).flatMap {
      case Right(response) =>
        response.rates.headOption match {
          case Some(exchangeRate) =>
            val rate = Rate(
              pair = pair,
              price = Price(exchangeRate.price),
              timestamp = Timestamp.now
            )
            cache.put[String, Rate](key(pair), rate) *>
              rate.asRight[RatesServiceError].pure[F]
          case None =>
            val error: RatesServiceError =
              RatesServiceError.OneFrameLookupFailed(s"No rate found for pair ${pair.from}${pair.to}")
            error.asLeft[Rate].pure[F]
        }
      case Left(error) =>
        val serviceError: RatesServiceError = RatesServiceError.OneFrameLookupFailed(error.toString)
        serviceError.asLeft[Rate].pure[F]
    }

  private def key(pair: Rate.Pair): String = s"${pair.from}_${pair.to}"

  private def isValid(timestamp: Timestamp): Boolean = {
    val now  = Timestamp.now
    val diff = now.value.toEpochSecond - timestamp.value.toEpochSecond
    diff < ttl.toSeconds
  }
}

object Service {
  def apply[F[_]: Concurrent](
      oneFrameClient: OneFrameClient[F],
      cache: CacheAlgebra[F],
      ttl: FiniteDuration
  ): Algebra[F] =
    new Service[F](oneFrameClient, cache, ttl)
}
