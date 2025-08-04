package forex.domain.currency

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CurrencySpec extends AnyFunSuite with Matchers {

  test("fromString returns correct Currency for supported codes") {
    Currency.fromString("USD") shouldBe Right(Currency.USD)
    Currency.fromString("eur") shouldBe Right(Currency.EUR)
    Currency.fromString("jPy") shouldBe Right(Currency.JPY)
  }

  test("fromString is case-insensitive") {
    Currency.fromString("cAd") shouldBe Right(Currency.CAD)
    Currency.fromString("nzd") shouldBe Right(Currency.NZD)
  }

  test("fromString returns Left(CurrencyError.Empty) for empty string") {
    Currency.fromString("") shouldBe Left(CurrencyError.Empty)
    Currency.fromString("   ") shouldBe Left(CurrencyError.Empty)
  }

  test("fromString returns Left(CurrencyError.Unsupported) for unsupported code") {
    val res = Currency.fromString("XXX")
    res should matchPattern { case Left(CurrencyError.Unsupported("XXX")) => }
  }

  test("supported contains all valid currency codes") {
    Currency.supported should contain allOf ("AUD", "CAD", "CHF", "EUR", "GBP", "NZD", "JPY", "SGD", "USD")
  }
}
