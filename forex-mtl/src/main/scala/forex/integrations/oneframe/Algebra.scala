package forex.integrations.oneframe

import errors._
import forex.domain.rates.Rate
import forex.integrations.oneframe.Protocol.GetRateResponse

trait Algebra[F[_]] {
  def getRates(pairs: List[Rate.Pair]): F[OneFrameError Either GetRateResponse]
}
