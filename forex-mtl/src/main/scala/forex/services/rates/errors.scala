package forex.services.rates

object errors {

  sealed trait RatesServiceError extends Error
  object RatesServiceError {
    final case class OneFrameLookupFailed(msg: String) extends RatesServiceError
  }
}
