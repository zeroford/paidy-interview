package forex.domain.cache

sealed trait FetchStrategy
object FetchStrategy {
  case object MostUsed extends FetchStrategy
  case object All extends FetchStrategy
}
