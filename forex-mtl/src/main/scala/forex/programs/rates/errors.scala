package forex.programs.rates

import forex.services.rates.errors.RatesServiceError

object errors {

  sealed trait RateProgramError extends Error
  object RateProgramError {
    final case class RateLookupFailed(msg: String) extends RateProgramError
  }

  def toProgramError(error: Error): RateProgramError = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => RateProgramError.RateLookupFailed(msg)
  }
}
