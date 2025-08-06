package forex.http.rates

import forex.domain.currency.Currency
import org.http4s.ParseFailure
import cats.data.{ NonEmptyList, Validated, ValidatedNel }
import munit.FunSuite

class QueryValidatorSpec extends FunSuite {

  def validCur(cur: Currency): ValidatedNel[ParseFailure, Currency] =
    Validated.Valid(cur)

  def invalidCur(msg: String = "error"): ValidatedNel[ParseFailure, Currency] =
    Validated.Invalid(NonEmptyList.one(ParseFailure(msg, msg)))

  test("validate returns Valid for both params present and valid") {
    val res = QueryValidator.validate(
      Some(validCur(Currency.USD)),
      Some(validCur(Currency.JPY))
    )
    assertEquals(res, Validated.Valid((Currency.USD, Currency.JPY)))
  }

  test("validate returns Invalid if 'from' missing") {
    val res = QueryValidator.validate(
      None,
      Some(validCur(Currency.JPY))
    )
    assertEquals(res, Validated.Invalid(NonEmptyList.one("Missing 'from' query parameter")))
  }

  test("validate returns Invalid if 'to' missing") {
    val res = QueryValidator.validate(
      Some(validCur(Currency.USD)),
      None
    )
    assertEquals(res, Validated.Invalid(NonEmptyList.one("Missing 'to' query parameter")))
  }

  test("validate returns Invalid if param present but is error") {
    val res = QueryValidator.validate(
      Some(invalidCur("fail-from")),
      Some(validCur(Currency.JPY))
    )
    res match {
      case Validated.Invalid(nel) =>
        assert(nel.head.contains("Invalid 'from'"))
        assert(nel.head.contains("fail-from"))
      case Validated.Valid(_) => fail("Expected Invalid")
    }
  }

  test("validate accumulates multiple errors") {
    val res = QueryValidator.validate(
      Some(invalidCur("fail-from")),
      Some(invalidCur("fail-to"))
    )
    res match {
      case Validated.Invalid(nel) =>
        assertEquals(nel.size, 2)
        assert(nel.head.contains("Invalid 'from'"))
        assert(nel.tail.head.contains("Invalid 'to'"))
      case Validated.Valid(_) => fail("Expected Invalid")
    }
  }
}
