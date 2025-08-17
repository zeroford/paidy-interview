package forex.domain.rates

import forex.domain.currency.Currency
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }

import java.time.Instant

final case class PivotRate(currency: Currency, price: Price, timestamp: Timestamp)

object PivotRate {

  def default(currency: Currency, now: Instant): PivotRate =
    PivotRate(currency, Price(BigDecimal(1)), Timestamp(now))

  def fromResponse(currency: Currency, price: BigDecimal, time_stamp: Instant): PivotRate =
    PivotRate(currency, Price(price), Timestamp(time_stamp))

  implicit val pairDecoder: Decoder[PivotRate] = deriveDecoder[PivotRate]
  implicit val pairEncoder: Encoder[PivotRate] = deriveEncoder[PivotRate]
}
