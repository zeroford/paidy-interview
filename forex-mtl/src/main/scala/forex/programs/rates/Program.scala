package forex.programs.rates

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._
import errors._
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.services.RatesService

class Program[F[_]: Applicative](ratesService: RatesService[F]) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[RateProgramError Either Rate] = {
    val pair = Rate.Pair(request.from, request.to)

    if (pair.from == pair.to) {
      Rate(pair, Price(1.0), Timestamp.now)
        .asRight[RateProgramError]
        .pure[F]
    } else {
      EitherT(ratesService.get(pair))
        .leftMap(toProgramError)
        .value
    }
  }

}

object Program {
  def apply[F[_]: Applicative](ratesService: RatesService[F]): Algebra[F] = new Program[F](ratesService)
}
