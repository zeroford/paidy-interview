package forex.domain.currency

import munit.FunSuite
import forex.domain.currency.errors.CurrencyError

class CurrencySpec extends FunSuite {

  test("fromString returns correct Currency for supported codes") {
    assertEquals(Currency.fromString("USD"), Right(Currency.USD))
    assertEquals(Currency.fromString("eur"), Right(Currency.EUR))
    assertEquals(Currency.fromString("jPy"), Right(Currency.JPY))
  }

  test("fromString is case-insensitive") {
    assertEquals(Currency.fromString("cAd"), Right(Currency.CAD))
    assertEquals(Currency.fromString("nzd"), Right(Currency.NZD))
  }

  test("fromString returns Left(CurrencyError.Empty) for empty string") {
    assertEquals(Currency.fromString(""), Left(CurrencyError.Empty()))
    assertEquals(Currency.fromString("   "), Left(CurrencyError.Empty()))
  }

  test("fromString returns Left(CurrencyError.Unsupported) for unsupported code") {
    val res = Currency.fromString("ABC")
    assertEquals(res, Left(CurrencyError.Unsupported("ABC")))
  }
}
