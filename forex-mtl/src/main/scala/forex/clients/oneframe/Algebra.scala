package forex.clients.oneframe

import forex.domain.rates.Rate
import forex.clients.oneframe.Protocol.OneFrameRatesResponse
import forex.domain.error.AppError

trait Algebra[F[_]] {
  def getRates(pairs: List[Rate.Pair]): F[AppError Either OneFrameRatesResponse]
}
