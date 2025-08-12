package forex.clients.oneframe

import errors._
import forex.domain.rates.Rate
import forex.clients.oneframe.Protocol.OneFrameRatesResponse

trait Algebra[F[_]] {
  def getRates(pairs: List[Rate.Pair]): F[OneFrameError Either OneFrameRatesResponse]
}
