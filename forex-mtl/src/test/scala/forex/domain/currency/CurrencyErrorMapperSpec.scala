package forex.domain.currency

import munit.FunSuite

class CurrencyErrorMapperSpec extends FunSuite {

  test("toParseFailure returns ParseFailure for Unsupported currency") {
    val err = CurrencyError.Unsupported("ABC")
    val pf  = CurrencyErrorMapper.toParseFailure(err)

    assertEquals(pf.sanitized, "Invalid currency")
    assert(pf.details.contains("ABC"))
    assert(pf.details.matches(".*(AUD|CAD|CHF|EUR|GBP|NZD|JPY|SGD|USD).*"))
  }

  test("toParseFailure returns ParseFailure for Empty currency") {
    val pf = CurrencyErrorMapper.toParseFailure(CurrencyError.Empty)
    assertEquals(pf.sanitized, "Empty currency")
    assert(pf.details.contains("must not be empty"))
  }
}
