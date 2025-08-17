package forex.http.rates

import cats.data.{ Validated, ValidatedNel }
import cats.syntax.all._
import forex.domain.currency.Currency
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
    ).tupled.andThen { case (from, to) =>
      if (from == to) {
        Validated.invalidNel("Same currency not allowed for 'from' and 'to' parameters")
      } else {
        Validated.valid((from, to))
      }
    }

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
