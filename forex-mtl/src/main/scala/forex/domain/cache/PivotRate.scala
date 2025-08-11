package forex.domain.cache

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Timestamp }
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

import java.time.OffsetDateTime

final case class PivotRate(currency: Currency, price: Price, timestamp: Timestamp)

object PivotRate {
  implicit val pairDecoder: Decoder[PivotRate] = deriveDecoder[PivotRate]
  implicit val pairEncoder: Encoder[PivotRate] = deriveEncoder[PivotRate]

  def default(currency: Currency): PivotRate =
    PivotRate(currency, Price(BigDecimal(1)), Timestamp.now)

  def fromResponse(currency: Currency, price: BigDecimal, time_stamp: String): PivotRate =
    PivotRate(currency, Price(price), Timestamp(OffsetDateTime.parse(time_stamp)))
}
