package forex.domain.currency

import org.http4s.ParseFailure

sealed trait CurrencyError extends Exception

object CurrencyError {
  case object Empty extends CurrencyError
  case class Unsupported(currency: String) extends CurrencyError
}

object CurrencyErrorMapper {
  def toParseFailure(error: CurrencyError): ParseFailure = error match {
    case CurrencyError.Unsupported(invalid) =>
      ParseFailure(
        "Invalid currency",
        s"'$invalid' is not supported. Supported currencies: " +
          Currency.supported.mkString(", ")
      )
    case CurrencyError.Empty =>
      ParseFailure("Empty currency", "Currency parameter must not be empty.")
  }
}