package forex.clients.oneframe.interpreter

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.rates.Rate
import forex.clients.oneframe.Protocol.{OneFrameRate, OneFrameRatesResponse}
import forex.clients.oneframe.Algebra
import forex.clients.oneframe.errors.OneFrameError

class MockClient[F[_]: Applicative] extends Algebra[F] {

  override def getRates(pairs: List[Rate.Pair]): F[OneFrameError Either OneFrameRatesResponse] =
    pairs
      .map { pair =>
        OneFrameRate(
          from = pair.from.toString,
          to = pair.to.toString,
          bid = BigDecimal(100),
          ask = BigDecimal(100),
          price = BigDecimal(100),
          time_stamp = "2025-01-01T00:00:00Z"
        )
      }
      .toList
      .asRight[OneFrameError]
      .pure[F]

}

object MockClient {
  def apply[F[_]: Applicative]: Algebra[F] = new MockClient[F]
}
