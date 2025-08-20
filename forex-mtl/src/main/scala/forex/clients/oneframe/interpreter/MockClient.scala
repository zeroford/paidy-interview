package forex.clients.oneframe.interpreter

import java.time.Instant

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import org.typelevel.log4cats.Logger

import forex.clients.oneframe.Algebra
import forex.domain.error.AppError
import forex.domain.rates.{ PivotRate, Price, Rate, Timestamp }

class MockClient[F[_]: Applicative: Logger] extends Algebra[F] {

  override def getRates(pairs: List[Rate.Pair]): F[AppError Either List[PivotRate]] = {
    Logger[F].info(s"[MockOneFrame] Get rate from mock client")
    pairs
      .map { pair =>
        PivotRate(
          currency = pair.to,
          price = Price(BigDecimal(100)),
          timestamp = Timestamp(Instant.parse("2025-01-01T00:00:00Z"))
        )
      }
      .asRight[AppError]
      .pure[F]
  }
}

object MockClient {
  def apply[F[_]: Applicative: Logger]: Algebra[F] = new MockClient[F]
}
