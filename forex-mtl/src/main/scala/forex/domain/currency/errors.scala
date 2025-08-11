package forex.domain.currency

import org.http4s.ParseFailure

object errors {
  sealed trait CurrencyError extends Error
  object CurrencyError {
    final case class InvalidFormat() extends CurrencyError
    final case class Empty() extends CurrencyError
    final case class Unsupported(currency: String) extends CurrencyError

    def toParseFailure(error: CurrencyError): ParseFailure = error match {
      case CurrencyError.InvalidFormat() => ParseFailure("InvalidFormat", "Format of currency is invalid.")
      case CurrencyError.Empty()         => ParseFailure("Empty currency", "Currency parameter must not be empty.")
      case CurrencyError.Unsupported(currency) => ParseFailure("Invalid currency", s"'$currency' is not supported.")
      case _ => ParseFailure("Unknown error", "An unknown error occurred while parsing currency.")
    }
  }
}
