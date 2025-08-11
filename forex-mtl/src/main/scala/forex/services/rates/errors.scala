package forex.services.rates

object errors {

  sealed trait RatesServiceError extends Error
  object RatesServiceError {
    final case class OneFrameLookupFailed(msg: String) extends RatesServiceError
    final case class InvalidCurrencyError(currency: String) extends RatesServiceError
    final case class CacheOperationFailed(msg: String) extends RatesServiceError
    final case class CrossRateCalculationFailed(pair: String) extends RatesServiceError
  }

  def fromError(error: Error): RatesServiceError =
    error match {
      case e: RatesServiceError => e
      case _                    => RatesServiceError.OneFrameLookupFailed(error.getMessage)
    }
}
