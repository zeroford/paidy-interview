package forex.domain.rates

import forex.domain.cache.PivotRate
import forex.domain.currency.Currency
import forex.domain.currency.Currency.USD
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

  def fromPivotRate(pivotBase: PivotRate, pivotQuote: PivotRate): Rate = {
    val (price, timestamp) = (pivotBase.currency, pivotQuote.currency) match {
      case (USD, _) =>
        (pivotQuote.price.value, pivotQuote.timestamp)
      case (_, USD) =>
        (1.0 / pivotBase.price.value, pivotBase.timestamp)
      case (_, _) =>
        (pivotQuote.price.value / pivotBase.price.value, Timestamp.olderTTL(pivotBase.timestamp, pivotQuote.timestamp))
    }

    Rate(
      pair = Rate.Pair(pivotBase.currency, pivotQuote.currency),
      price = Price(price),
      timestamp = timestamp
    )
  }

  implicit val pairDecoder: Decoder[Pair] = deriveDecoder[Pair]
  implicit val pairEncoder: Encoder[Pair] = deriveEncoder[Pair]
  implicit val rateDecoder: Decoder[Rate] = deriveDecoder[Rate]
  implicit val rateEncoder: Encoder[Rate] = deriveEncoder[Rate]
}
