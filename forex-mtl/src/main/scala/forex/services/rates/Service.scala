package forex.services.rates

import cats.effect.Concurrent
import cats.syntax.all._
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.integrations.OneFrameClient
import forex.services.rates.errors.RatesServiceError

class Service[F[_]: Concurrent](oneFrameClient: OneFrameClient[F]) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    oneFrameClient.getRate(pair).map {
      case Right(response) =>
        response.rates.headOption match {
          case Some(exchangeRate) =>
            val rate = Rate(
              pair = pair,
              price = Price(exchangeRate.price),
              timestamp = Timestamp.now
            )
            rate.asRight[RatesServiceError]
          case None =>
            RatesServiceError.OneFrameLookupFailed(s"No rate found for pair ${pair.from}${pair.to}").asLeft[Rate]
        }
      case Left(error) =>
        RatesServiceError.OneFrameLookupFailed(error.toString).asLeft[Rate]
    }
}

object Service {

  def apply[F[_]: Concurrent](oneFrameClient: OneFrameClient[F]): Algebra[F] = new Service[F](oneFrameClient)

}
