package forex.domain.currency

sealed trait CurrencyError extends Exception

object CurrencyError {
  case object Empty extends CurrencyError
  case class Unsupported(currency: String) extends CurrencyError
}