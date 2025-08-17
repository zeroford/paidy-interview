package forex.clients.oneframe

import forex.clients.oneframe.Protocol.OneFrameRatesResponse
import forex.domain.error.AppError
import forex.domain.rates.Rate

trait Algebra[F[_]] {
  def getRates(pairs: List[Rate.Pair]): F[AppError Either OneFrameRatesResponse]
}
