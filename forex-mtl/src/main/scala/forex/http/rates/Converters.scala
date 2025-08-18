package forex.http.rates

import forex.domain.currency.Currency
import forex.domain.rates.Rate

object Converters {
  import Protocol._

  private[rates] def toRatePair(from: Currency, to: Currency): Rate.Pair =
    Rate.Pair(from, to)

  private[rates] def fromRate(rate: Rate): GetApiResponse =
    GetApiResponse(
      from = rate.pair.from,
      to = rate.pair.to,
      price = rate.price.round(rate.pair.fractionalPip),
      timestamp = rate.timestamp
    )
}
