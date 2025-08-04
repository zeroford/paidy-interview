package forex.domain.currency

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CurrencyErrorMapperSpec extends AnyFunSuite with Matchers {

  test("toParseFailure returns ParseFailure for Unsupported currency") {
    val err = CurrencyError.Unsupported("XXX")
    val pf  = CurrencyErrorMapper.toParseFailure(err)

    pf.sanitized shouldBe "Invalid currency"
    pf.details should include("XXX")
    pf.details should include regex "(AUD|CAD|CHF|EUR|GBP|NZD|JPY|SGD|USD)"
  }

  test("toParseFailure returns ParseFailure for Empty currency") {
    val pf = CurrencyErrorMapper.toParseFailure(CurrencyError.Empty)
    pf.sanitized shouldBe "Empty currency"
    pf.details should include("must not be empty")
  }
}
