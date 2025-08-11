package forex.domain.rates

import forex.domain.cache.PivotRate
import forex.domain.currency.Currency
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

final case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  def fromPivotRate(pivotBase: PivotRate, pivotQuote: PivotRate): Rate =
    Rate(
      pair = Rate.Pair(pivotBase.currency, pivotQuote.currency),
      price = Price(pivotQuote.price.value / pivotBase.price.value),
      timestamp = Timestamp.olderTTL(pivotBase.timestamp, pivotQuote.timestamp)
    )

  implicit val pairDecoder: Decoder[Pair] = deriveDecoder[Pair]
  implicit val pairEncoder: Encoder[Pair] = deriveEncoder[Pair]
  implicit val rateDecoder: Decoder[Rate] = deriveDecoder[Rate]
  implicit val rateEncoder: Encoder[Rate] = deriveEncoder[Rate]
}
