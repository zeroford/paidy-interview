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
      raw: Option[ValidatedNel[ParseFailure, Currency]]
  ): ValidationResult[Currency] =
    raw match {
      case None =>
        s"Missing `$paramName` query parameter".invalidNel
      case Some(validated) =>
        validated.leftMap { nel =>
          nel.map(e => s"Invalid `$paramName`: ${e.sanitized} (${e.details})")
        }
    }
}
