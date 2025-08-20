package forex.domain.cache

import forex.domain.currency.Currency
import forex.domain.rates.Rate

sealed trait FetchStrategy
object FetchStrategy {
  case object MostUsed extends FetchStrategy
  case object LeastUsed extends FetchStrategy
  case object All extends FetchStrategy

  def fromPair(pair: Rate.Pair, needBase: Boolean = true, needQuote: Boolean = true): FetchStrategy = {

    val baseMostUsed  = Currency.isMostUsed(pair.from)
    val quoteMostUsed = Currency.isMostUsed(pair.to)

    val anyMost  = (needBase && baseMostUsed) || (needQuote && quoteMostUsed)
    val anyLeast = (needBase && !baseMostUsed) || (needQuote && !quoteMostUsed)

    (anyMost, anyLeast) match {
      case (true, false) => FetchStrategy.MostUsed
      case (false, true) => FetchStrategy.LeastUsed
      case _             => FetchStrategy.All
    }
  }
}
