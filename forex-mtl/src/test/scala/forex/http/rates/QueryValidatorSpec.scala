package forex.http.rates

import cats.data.{ NonEmptyList, Validated, ValidatedNel }
import munit.FunSuite
import org.http4s.ParseFailure

import forex.domain.currency.Currency

class QueryValidatorSpec extends FunSuite {

  private def validCur(cur: Currency): ValidatedNel[ParseFailure, Currency] =
    Validated.Valid(cur)

  private def invalidCur(msg: String): ValidatedNel[ParseFailure, Currency] =
    Validated.Invalid(NonEmptyList.one(ParseFailure(msg, msg)))

  test("validate should return Valid for both params present and valid") {
    val result = QueryValidator.validate(
      Some(validCur(Currency.USD)),
      Some(validCur(Currency.JPY))
    )
    assertEquals(result, Validated.Valid((Currency.USD, Currency.JPY)), "Valid currencies should be accepted")
  }

  test("validate should return Invalid if 'from' parameter is missing") {
    val result = QueryValidator.validate(
      None,
      Some(validCur(Currency.JPY))
    )
    assertEquals(
      result,
      Validated.Invalid(NonEmptyList.one("Missing 'from' query parameter")),
      "Missing 'from' should be rejected"
    )
  }

  test("validate should return Invalid if 'to' parameter is missing") {
    val result = QueryValidator.validate(
      Some(validCur(Currency.USD)),
      None
    )
    assertEquals(
      result,
      Validated.Invalid(NonEmptyList.one("Missing 'to' query parameter")),
      "Missing 'to' should be rejected"
    )
  }

  test("validate should return Invalid if 'from' parameter is present but invalid") {
    val result = QueryValidator.validate(
      Some(invalidCur("fail-from")),
      Some(validCur(Currency.JPY))
    )
    result match {
      case Validated.Invalid(nel) =>
        assert(nel.head.contains("Invalid 'from'"), "Error should mention 'from' parameter")
        assert(nel.head.contains("fail-from"), "Error should contain the original failure message")
      case Validated.Valid(_) => fail("Expected Invalid result for invalid 'from' parameter")
    }
  }

  test("validate should return Invalid if 'to' parameter is present but invalid") {
    val result = QueryValidator.validate(
      Some(validCur(Currency.USD)),
      Some(invalidCur("fail-to"))
    )
    result match {
      case Validated.Invalid(nel) =>
        assert(nel.head.contains("Invalid 'to'"), "Error should mention 'to' parameter")
        assert(nel.head.contains("fail-to"), "Error should contain the original failure message")
      case Validated.Valid(_) => fail("Expected Invalid result for invalid 'to' parameter")
    }
  }

  test("validate should accumulate multiple errors when both parameters are invalid") {
    val result = QueryValidator.validate(
      Some(invalidCur("fail-from")),
      Some(invalidCur("fail-to"))
    )
    result match {
      case Validated.Invalid(nel) =>
        assertEquals(nel.size, 2, "Should accumulate both validation errors")
        assert(nel.head.contains("Invalid 'from'"), "First error should mention 'from' parameter")
        assert(nel.tail.head.contains("Invalid 'to'"), "Second error should mention 'to' parameter")
      case Validated.Valid(_) => fail("Expected Invalid result for multiple invalid parameters")
    }
  }

  test("validate should handle all valid currency combinations") {
    val currencies = List(
      Currency.USD,
      Currency.EUR,
      Currency.GBP,
      Currency.JPY,
      Currency.CAD,
      Currency.CHF,
      Currency.AUD,
      Currency.NZD
    )

    currencies.combinations(2).foreach {
      case List(from, to) =>
        val result = QueryValidator.validate(
          Some(validCur(from)),
          Some(validCur(to))
        )
        assert(result.isValid, s"Valid currency pair $from-$to should be accepted")
      case _ =>
    }
  }

  test("validate should reject same currency for from and to") {
    val result = QueryValidator.validate(
      Some(validCur(Currency.USD)),
      Some(validCur(Currency.USD))
    )
    result match {
      case Validated.Invalid(nel) =>
        assert(nel.head.contains("Same currency not allowed"), "Should reject same currency for both parameters")
      case Validated.Valid(_) => fail("Expected Invalid result for same currency")
    }
  }
}
