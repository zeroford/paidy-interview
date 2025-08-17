package forex.domain.currency

import munit.FunSuite

import forex.domain.currency.errors.CurrencyError

class CurrencySpec extends FunSuite {

  test("fromString returns correct Currency for supported codes") {
    assertEquals(Currency.fromString("EUR"), Right(Currency.EUR))
    assertEquals(Currency.fromString("jPy"), Right(Currency.JPY))
    assertEquals(Currency.fromString("GBP"), Right(Currency.GBP))
  }

  test("fromString is case-insensitive") {
    assertEquals(Currency.fromString("eUr"), Right(Currency.EUR))
    assertEquals(Currency.fromString("jpy"), Right(Currency.JPY))
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
