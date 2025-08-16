package forex.services.rates.concurrent

sealed trait RatesBucket
object RatesBucket {
  case object MostUsed extends RatesBucket
  case object LeastUsed extends RatesBucket
}
