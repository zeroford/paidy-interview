package forex.http.util

import cats.data._
import forex.domain.currency.Currency
import cats.syntax.all._
import org.http4s.ParseFailure

object QueryValidator {

  private type ValidationResult[A] = ValidatedNel[String, A]

  def validate(
      fromOpt: Option[ValidatedNel[ParseFailure, Currency]],
      toOpt: Option[ValidatedNel[ParseFailure, Currency]]
  ): ValidationResult[(Currency, Currency)] =
    (
      validateCurrencyParam("from", fromOpt),
      validateCurrencyParam("to", toOpt)
    ).tupled

  private def validateCurrencyParam(
      paramName: String,
      optValue: Option[ValidatedNel[ParseFailure, Currency]]
  ): ValidationResult[Currency] =
    optValue match {
      case None =>
        Validated.invalidNel(s"Missing '$paramName' query parameter")
      case Some(validated) =>
        validated.leftMap(_.map(e => s"Invalid '$paramName': ${e.sanitized} (${e.details})"))
    }
}
