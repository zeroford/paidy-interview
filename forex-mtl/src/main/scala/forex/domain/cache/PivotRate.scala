package forex.domain.cache

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Timestamp }
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

final case class PivotRate(currency: Currency, price: Price, timestamp: Timestamp)

object PivotRate {
  implicit val pairDecoder: Decoder[PivotRate] = deriveDecoder[PivotRate]
  implicit val pairEncoder: Encoder[PivotRate] = deriveEncoder[PivotRate]
}
