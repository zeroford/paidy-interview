package forex.programs.rates

import cats.Applicative
import cats.syntax.all._
import forex.domain.error.AppError
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.services.RatesService

class Program[F[_]: Applicative](ratesService: RatesService[F]) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[AppError Either Rate] = {
    val pair = Rate.Pair(request.from, request.to)

    if (pair.from == pair.to) {
      Rate(pair, Price(1.0), Timestamp.now)
        .asRight[AppError]
        .pure[F]
    } else {
      ratesService.get(pair)
    }
  }

}

object Program {
  def apply[F[_]: Applicative](ratesService: RatesService[F]): Algebra[F] = new Program[F](ratesService)
}
