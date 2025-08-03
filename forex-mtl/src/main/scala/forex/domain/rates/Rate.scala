package forex.domain.rates

import forex.domain.currency.Currency
import forex.domain.{Price, Timestamp}

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )
}
