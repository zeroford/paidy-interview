package forex.domain.currency

import munit.FunSuite

import forex.domain.currency.errors.CurrencyError

class ErrorsSpec extends FunSuite {

  test("toParseFailure returns ParseFailure for Unsupported currency") {
    val err = CurrencyError.Unsupported("ABC")
    val pf  = errors.CurrencyError.toParseFailure(err)

    assertEquals(pf.sanitized, "Invalid currency")
    assert(pf.details.contains("ABC"))
    assert(pf.details.contains("not supported"))
  }

  test("toParseFailure returns ParseFailure for Empty currency") {
    val pf = errors.CurrencyError.toParseFailure(CurrencyError.Empty())
    assertEquals(pf.sanitized, "Empty currency")
    assert(pf.details.contains("must not be empty"))
  }

  test("toParseFailure returns ParseFailure for InvalidFormat currency") {
    val pf = errors.CurrencyError.toParseFailure(CurrencyError.InvalidFormat())
    assertEquals(pf.sanitized, "InvalidFormat")
    assert(pf.details.contains("Currency format must be 3 letters"))
  }

  test("toParseFailure handles all CurrencyError cases") {
    // Test all known cases
    val unsupported   = errors.CurrencyError.toParseFailure(CurrencyError.Unsupported("XYZ"))
    val empty         = errors.CurrencyError.toParseFailure(CurrencyError.Empty())
    val invalidFormat = errors.CurrencyError.toParseFailure(CurrencyError.InvalidFormat())

    assertEquals(unsupported.sanitized, "Invalid currency")
    assertEquals(empty.sanitized, "Empty currency")
    assertEquals(invalidFormat.sanitized, "InvalidFormat")
  }

}
