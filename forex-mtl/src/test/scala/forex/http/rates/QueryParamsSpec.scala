package forex.http.rates

import forex.domain.currency.Currency
import org.http4s.QueryParameterValue
import cats.data.Validated
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class QueryParamsSpec extends AnyFunSuite with Matchers {

  test("currencyQueryParamDecoder decodes valid currency") {
    val decoder = QueryParams.currencyQueryParamDecoder

    decoder.decode(QueryParameterValue("USD")) match {
      case Validated.Valid(cur)   => cur shouldBe Currency.USD
      case Validated.Invalid(err) => fail(s"Expected Valid, got Invalid: $err")
    }
    decoder.decode(QueryParameterValue("eur")) match {
      case Validated.Valid(cur)   => cur shouldBe Currency.EUR
      case Validated.Invalid(err) => fail(s"Expected Valid, got Invalid: $err")
    }
    decoder.decode(QueryParameterValue("jPy")) match {
      case Validated.Valid(cur)   => cur shouldBe Currency.JPY
      case Validated.Invalid(err) => fail(s"Expected Valid, got Invalid: $err")
    }
  }

  test("currencyQueryParamDecoder returns failure for empty currency") {
    val decoder = QueryParams.currencyQueryParamDecoder

    decoder.decode(QueryParameterValue("")) match {
      case Validated.Invalid(errNel) =>
        val err = errNel.head
        err.sanitized shouldBe "Empty currency"
        err.details should include("must not be empty")
      case Validated.Valid(cur) => fail(s"Expected Invalid, got Valid: $cur")
    }
  }

  test("currencyQueryParamDecoder returns failure for unsupported currency") {
    val decoder = QueryParams.currencyQueryParamDecoder

    decoder.decode(QueryParameterValue("XXX")) match {
      case Validated.Invalid(errNel) =>
        val err = errNel.head
        err.sanitized shouldBe "Invalid currency"
        err.details should include("XXX")
        err.details should include regex "(AUD|USD|EUR|GBP|JPY|SGD|NZD|CHF|CAD)"
      case Validated.Valid(cur) => fail(s"Expected Invalid, got Valid: $cur")
    }
  }
}
