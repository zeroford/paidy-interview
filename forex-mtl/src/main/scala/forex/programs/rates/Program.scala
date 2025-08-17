package forex.programs.rates

import cats.Applicative
import cats.effect.Clock
import cats.syntax.all._
import forex.domain.error.AppError
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.services.RatesService

final class Program[F[_]: Applicative: Clock](ratesService: RatesService[F]) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[AppError Either Rate] = {
    val pair = Rate.Pair(request.from, request.to)

    if (pair.from == pair.to) {
      Timestamp.now[F].map { ts =>
        Rate(pair, Price(1.0), ts).asRight[AppError]
      }
    } else {
      ratesService.get(pair)
    }
  }

}

object Program {
  def apply[F[_]: Applicative: Clock](ratesService: RatesService[F]): Algebra[F] = new Program[F](ratesService)
}
