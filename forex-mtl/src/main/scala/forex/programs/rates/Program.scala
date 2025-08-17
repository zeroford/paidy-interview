package forex.programs.rates

import cats.Applicative
import cats.effect.Clock
import cats.syntax.all._
import forex.domain.error.AppError
import forex.domain.rates.Rate
import forex.services.RatesService

final class Program[F[_]: Applicative: Clock](ratesService: RatesService[F]) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[AppError Either Rate] =
    if (request.from == request.to) {
      Rate.default[F](request.from).map(_.asRight[AppError])
    } else {
      val pair = Rate.Pair(request.from, request.to)
      ratesService.get(pair)
    }

}

object Program {
  def apply[F[_]: Applicative: Clock](ratesService: RatesService[F]): Algebra[F] = new Program[F](ratesService)
}
