package forex.integrations.oneframe.interpreter

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.rates.Rate
import forex.integrations.oneframe.Protocol.{ ExchangeRate, GetRateResponse }
import forex.integrations.oneframe.Algebra
import forex.integrations.oneframe.errors.OneFrameError

class DummyClient[F[_]: Applicative] extends Algebra[F] {

  override def getRate(pair: Rate.Pair): F[OneFrameError Either GetRateResponse] =
    GetRateResponse(
      ExchangeRate(
        from = pair.from.toString,
        to = pair.to.toString,
        bid = BigDecimal(100),
        ask = BigDecimal(100),
        price = BigDecimal(100),
        time_stamp = "2025-08-06T00:00:00Z"
      ) :: Nil
    ).asRight[OneFrameError].pure[F]

}

object DummyClient {
  def apply[F[_]: Applicative]: Algebra[F] = new DummyClient[F]
}
