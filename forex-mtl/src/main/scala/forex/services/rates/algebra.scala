package forex.services.rates

import forex.domain.rates.Rate
import errors._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[RatesServiceError Either Rate]
}
