package forex.http.rates

import forex.domain.currency.Currency
import org.http4s.QueryParameterValue
import cats.data.Validated
import munit.FunSuite

class QueryParamsSpec extends FunSuite {

  test("currencyQueryParamDecoder decodes valid currency") {
    val decoder = QueryParams.currencyQueryParamDecoder

    decoder.decode(QueryParameterValue("USD")) match {
      case Validated.Valid(cur)   => assertEquals(cur, Currency.USD)
      case Validated.Invalid(err) => fail(s"Expected Valid, got Invalid: $err")
    }
    decoder.decode(QueryParameterValue("eur")) match {
      case Validated.Valid(cur)   => assertEquals(cur, Currency.EUR)
      case Validated.Invalid(err) => fail(s"Expected Valid, got Invalid: $err")
    }
    decoder.decode(QueryParameterValue("jPy")) match {
      case Validated.Valid(cur)   => assertEquals(cur, Currency.JPY)
      case Validated.Invalid(err) => fail(s"Expected Valid, got Invalid: $err")
    }
  }

  test("currencyQueryParamDecoder returns failure for empty currency") {
    val decoder = QueryParams.currencyQueryParamDecoder

    decoder.decode(QueryParameterValue("")) match {
      case Validated.Invalid(errNel) =>
        val err = errNel.head
        assertEquals(err.sanitized, "Empty currency")
        assert(err.details.contains("must not be empty"))
      case Validated.Valid(cur) => fail(s"Expected Invalid, got Valid: $cur")
    }
  }

  test("currencyQueryParamDecoder returns failure for unsupported currency") {
    val decoder = QueryParams.currencyQueryParamDecoder

    decoder.decode(QueryParameterValue("XXX")) match {
      case Validated.Invalid(errNel) =>
        val err = errNel.head
        assertEquals(err.sanitized, "Invalid currency")
        assert(err.details.contains("XXX"))
        assert(err.details.contains("not supported"))
      case Validated.Valid(cur) => fail(s"Expected Invalid, got Valid: $cur")
    }
  }
}
