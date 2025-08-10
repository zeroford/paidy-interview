package forex.programs.rates

import forex.services.rates.errors.RatesServiceError

object errors {

  sealed trait RateProgramError extends Error
  object RateProgramError {
    final case class RateLookupFailed(msg: String) extends RateProgramError
    final case class ValidationFailed(errors: List[String]) extends RateProgramError
  }

  def toProgramError(error: RatesServiceError): RateProgramError =
    error match {
      case RatesServiceError.OneFrameLookupFailed(msg) =>
        RateProgramError.RateLookupFailed(msg)
      case RatesServiceError.InvalidCurrencyError(currency) =>
        RateProgramError.RateLookupFailed(s"Invalid currency: $currency")
      case RatesServiceError.CacheOperationFailed(msg) =>
        RateProgramError.RateLookupFailed(s"Cache operation failed: $msg")
      case RatesServiceError.CrossRateCalculationFailed(pair) =>
        RateProgramError.RateLookupFailed(s"Cross rate calculation failed for pair: $pair")
    }
}
