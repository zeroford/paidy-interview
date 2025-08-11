package forex.integrations.oneframe.interpreter

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.rates.Rate
import forex.integrations.oneframe.Protocol.{ ExchangeRate, GetRateResponse }
import forex.integrations.oneframe.Algebra
import forex.integrations.oneframe.errors.OneFrameError

class MockClient[F[_]: Applicative] extends Algebra[F] {

  override def getRates(pairs: List[Rate.Pair]): F[OneFrameError Either GetRateResponse] =
    GetRateResponse(
      pairs.map { pair =>
        ExchangeRate(
          from = pair.from.toString,
          to = pair.to.toString,
          bid = BigDecimal(100),
          ask = BigDecimal(100),
          price = BigDecimal(100),
          time_stamp = "2025-01-01T00:00:00Z"
        )
      }
    ).asRight[OneFrameError].pure[F]

}

object MockClient {
  def apply[F[_]: Applicative]: Algebra[F] = new MockClient[F]
}
