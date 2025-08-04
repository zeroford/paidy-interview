package forex.http.rates

import forex.domain.currency.Currency
import org.http4s.ParseFailure
import cats.data.{Validated, ValidatedNel, NonEmptyList}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class QueryValidatorSpec extends AnyFunSuite with Matchers {

  def validCur(cur: Currency): ValidatedNel[ParseFailure, Currency] =
    Validated.Valid(cur)

  def invalidCur(msg: String = "error"): ValidatedNel[ParseFailure, Currency] =
    Validated.Invalid(NonEmptyList.one(ParseFailure(msg, msg)))

  test("validate returns Valid for both params present and valid") {
    val res = QueryValidator.validate(
      Some(validCur(Currency.USD)),
      Some(validCur(Currency.JPY))
    )
    res shouldBe Validated.Valid((Currency.USD, Currency.JPY))
  }

  test("validate returns Invalid if 'from' missing") {
    val res = QueryValidator.validate(
      None,
      Some(validCur(Currency.JPY))
    )
    res shouldBe Validated.Invalid(NonEmptyList.one("Missing 'from' query parameter"))
  }

  test("validate returns Invalid if 'to' missing") {
    val res = QueryValidator.validate(
      Some(validCur(Currency.USD)),
      None
    )
    res shouldBe Validated.Invalid(NonEmptyList.one("Missing 'to' query parameter"))
  }

  test("validate returns Invalid if param present but is error") {
    val res = QueryValidator.validate(
      Some(invalidCur("fail-from")),
      Some(validCur(Currency.JPY))
    )
    res match {
      case Validated.Invalid(nel) =>
        nel.head should include ("Invalid 'from'")
        nel.head should include ("fail-from")
      case Validated.Valid(_) => fail("Expected Invalid")
    }
  }

  test("validate returns Invalid for both params missing") {
    val res = QueryValidator.validate(
      None,
      None
    )
    res.isInvalid shouldBe true
    res.fold(
      errors => {
        errors.toList should contain ("Missing 'from' query parameter")
        errors.toList should contain ("Missing 'to' query parameter")
      },
      _ => fail("Should not be valid")
    )
  }
}
