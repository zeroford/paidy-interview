package forex.domain.currency

import munit.FunSuite
import forex.domain.currency.errors.CurrencyError

class CurrencyErrorMapperSpec extends FunSuite {

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
}
