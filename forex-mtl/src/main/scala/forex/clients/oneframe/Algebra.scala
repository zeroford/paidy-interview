package forex.clients.oneframe

import forex.domain.error.AppError
import forex.domain.rates.{ PivotRate, Rate }

trait Algebra[F[_]] {
  def getRates(pairs: List[Rate.Pair]): F[AppError Either List[PivotRate]]
}
