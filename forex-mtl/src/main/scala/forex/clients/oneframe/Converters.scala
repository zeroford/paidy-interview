package forex.clients.oneframe

import cats.syntax.either._

import forex.clients.oneframe.Protocol.OneFrameRate
import forex.domain.currency.Currency
import forex.domain.rates.{ PivotRate, Price, Timestamp }
import forex.domain.error.AppError

object Converters {
  private[oneframe] def toPivotRate(res: OneFrameRate): Either[AppError, PivotRate] =
    Currency
      .fromString(res.to)
      .leftMap(e => AppError.Validation(s"Unsupported currency: ${res.to} - $e"))
      .map(c => PivotRate(c, Price(res.price), Timestamp(res.time_stamp)))
}
