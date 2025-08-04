package forex.http.util

import forex.domain.currency.{Currency, CurrencyError}
import org.http4s.ParseFailure

object CurrencyErrorMappings {
  def toParseFailure(error: CurrencyError): ParseFailure = error match {
    case CurrencyError.Unsupported(invalid) =>
      ParseFailure(
        "Invalid currency",
        s"'$invalid' is not supported. Supported currencies: ${Currency.supported.mkString(", ")}"
      )
    case CurrencyError.Empty =>
      ParseFailure("Empty currency", "Currency parameter must not be empty.")
  }
}