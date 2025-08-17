package forex.programs.rates

import cats.Monad
import cats.effect.Clock
import cats.syntax.all._
import forex.domain.error.AppError
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.services.RatesService

final class Program[F[_]: Monad: Clock](ratesService: RatesService[F]) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[AppError Either Rate] = {
    val pair = Rate.Pair(request.from, request.to)

    Clock[F].realTimeInstant.flatMap { now =>
      if (pair.from === pair.to)
        Rate(
          pair = pair,
          price = Price(1.0),
          timestamp = Timestamp(now)
        ).asRight[AppError].pure[F]
      else ratesService.get(pair, now)
    }
  }
}

object Program {
  def apply[F[_]: Monad: Clock](ratesService: RatesService[F]): Algebra[F] = new Program[F](ratesService)
}
