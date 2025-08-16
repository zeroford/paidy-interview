package forex.services.rates

import forex.domain.error.AppError
import forex.domain.rates.Rate

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[AppError Either Rate]
}
