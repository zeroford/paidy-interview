package forex.services.rates

import forex.domain.error.AppError
import forex.domain.rates.Rate

import java.time.Instant

trait Algebra[F[_]] {
  def get(pair: Rate.Pair, now: Instant): F[AppError Either Rate]
}
