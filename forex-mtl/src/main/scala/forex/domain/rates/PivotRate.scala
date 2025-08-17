package forex.domain.rates

import forex.domain.currency.Currency
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }

import java.time.{ OffsetDateTime, ZoneOffset }

final case class PivotRate(currency: Currency, price: Price, timestamp: Timestamp)

object PivotRate {
  implicit val pairDecoder: Decoder[PivotRate] = deriveDecoder[PivotRate]
  implicit val pairEncoder: Encoder[PivotRate] = deriveEncoder[PivotRate]

  def default(currency: Currency): PivotRate =
    PivotRate(currency, Price(BigDecimal(1)), Timestamp(OffsetDateTime.now(ZoneOffset.UTC)))

  def fromResponse(currency: Currency, price: BigDecimal, time_stamp: String): PivotRate =
    PivotRate(currency, Price(price), Timestamp(OffsetDateTime.parse(time_stamp)))
}
