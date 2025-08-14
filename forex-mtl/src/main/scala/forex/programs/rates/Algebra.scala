package forex.programs.rates

import forex.domain.error.AppError
import forex.domain.rates.Rate

trait Algebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[AppError Either Rate]
}
