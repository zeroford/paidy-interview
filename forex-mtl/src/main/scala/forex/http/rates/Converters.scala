package forex.http.rates

import scala.math.BigDecimal.RoundingMode

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate }

object Converters {
  import Protocol._

  private[rates] def toRatePair(from: Currency, to: Currency): Rate.Pair =
    Rate.Pair(from, to)

  private[rates] def fromRate(rate: Rate): GetApiResponse = {
    val scale      = fractionalPip(rate.pair)
    val roundPrice = Price(rate.price.value.setScale(scale, RoundingMode.HALF_UP))
    GetApiResponse(
      from = rate.pair.from,
      to = rate.pair.to,
      price = roundPrice,
      timestamp = rate.timestamp
    )
  }

  private def fractionalPip(pair: Rate.Pair): Int =
    if (Set(pair.from, pair.to).contains(Currency.JPY)) 3 else 5
}
