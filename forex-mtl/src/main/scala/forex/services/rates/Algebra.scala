package forex.services.rates

import java.time.Instant

import forex.domain.error.AppError
import forex.domain.rates.Rate

trait Algebra[F[_]] {
  def get(pair: Rate.Pair, now: Instant): F[AppError Either Rate]
}
