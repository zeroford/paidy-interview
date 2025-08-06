package forex.integrations.oneframe

import errors._
import forex.domain.rates.Rate
import forex.integrations.oneframe.Protocol.GetRateResponse

trait Algebra[F[_]] {
  def getRate(pair: Rate.Pair): F[OneFrameError Either GetRateResponse]
}
