package forex.domain.cache

import forex.domain.currency.Currency
import forex.domain.rates.Rate

sealed trait FetchStrategy
object FetchStrategy {
  case object MostUsed extends FetchStrategy
  case object All extends FetchStrategy

  def fromPair(pair: Rate.Pair, needBase: Boolean, needQuote: Boolean): FetchStrategy =
    if (needBase && !Currency.isMostUsed(pair.from)) { FetchStrategy.All }
    else if (needQuote && !Currency.isMostUsed(pair.to)) { FetchStrategy.All }
    else { FetchStrategy.MostUsed }
}
