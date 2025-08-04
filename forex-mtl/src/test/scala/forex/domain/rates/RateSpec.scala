package forex.domain.rates

import forex.domain.currency.Currency
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.time.OffsetDateTime

class RateSpec extends AnyFunSuite with Matchers {

  test("Rate should store price and price.value correctly (BigDecimal)") {
    val priceVal = BigDecimal("123.45")
    val rate     = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(priceVal),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    )

    rate.price.value shouldBe priceVal
  }

  test("Rate should store price from Integer via Price companion") {
    val priceInt: Integer = 42
    val rate              = Rate(
      pair = Rate.Pair(Currency.EUR, Currency.GBP),
      price = Price(priceInt),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T15:00:00Z"))
    )
    rate.price.value shouldBe BigDecimal(42)
  }

  test("Rate should be equal if all fields are equal") {
    val r1 = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal("1.1")),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    )
    val r2 = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal("1.1")),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    )
    r1 shouldBe r2
    r1.hashCode shouldBe r2.hashCode
  }

  test("Rate.Pair should store currencies correctly") {
    val pair = Rate.Pair(Currency.USD, Currency.CHF)
    pair.from shouldBe Currency.USD
    pair.to shouldBe Currency.CHF
  }

  test("Rate should store timestamp correctly") {
    val timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    val rate      = Rate(
      pair = Rate.Pair(Currency.USD, Currency.CHF),
      price = Price(BigDecimal(2.22)),
      timestamp = timestamp
    )
    rate.timestamp shouldBe timestamp
    rate.timestamp.value shouldBe OffsetDateTime.parse("2024-08-04T13:00:00Z")
  }

  test("Rate equality and hashCode include timestamp") {
    val t  = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    val r1 = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1.2), t)
    val r2 = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1.2), t)
    r1 shouldBe r2
    r1.hashCode shouldBe r2.hashCode

    val r3 =
      Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1.2), Timestamp(OffsetDateTime.parse("2025-01-01T01:00:00Z")))
    r3 should not be r1
  }
}
