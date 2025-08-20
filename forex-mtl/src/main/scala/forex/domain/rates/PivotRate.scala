package forex.domain.rates

import java.time.Instant

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

import forex.domain.currency.Currency

final case class PivotRate(currency: Currency, price: Price, timestamp: Timestamp)
object PivotRate {

  def default(currency: Currency, now: Instant): PivotRate =
    PivotRate(currency, Price(BigDecimal(1)), Timestamp(now))

  def fromResponse(currency: Currency, price: BigDecimal, time_stamp: Instant): PivotRate =
    PivotRate(currency, Price(price), Timestamp(time_stamp))

  implicit val pairDecoder: Decoder[PivotRate] = deriveDecoder[PivotRate]
  implicit val pairEncoder: Encoder[PivotRate] = deriveEncoder[PivotRate]
}
