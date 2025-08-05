package forex.domain.rates

import forex.domain.currency.Currency
import munit.FunSuite
import java.time.OffsetDateTime

class RateSpec extends FunSuite {

  test("Rate should store price and price.value correctly (BigDecimal)") {
    val priceVal = BigDecimal("123.45")
    val rate     = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(priceVal),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    )

    assertEquals(rate.price.value, priceVal)
  }

  test("Rate should store price from Integer via Price companion") {
    val priceInt: Integer = 42
    val rate              = Rate(
      pair = Rate.Pair(Currency.EUR, Currency.GBP),
      price = Price(priceInt),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T15:00:00Z"))
    )
    assertEquals(rate.price.value, BigDecimal(42))
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
    assertEquals(r1, r2)
    assertEquals(r1.hashCode, r2.hashCode)
  }

  test("Rate.Pair should store currencies correctly") {
    val pair = Rate.Pair(Currency.USD, Currency.CHF)
    assertEquals(pair.from, Currency.USD)
    assertEquals(pair.to, Currency.CHF)
  }

  test("Rate should store timestamp correctly") {
    val timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    val rate      = Rate(
      pair = Rate.Pair(Currency.USD, Currency.CHF),
      price = Price(BigDecimal(2.22)),
      timestamp = timestamp
    )
    assertEquals(rate.timestamp, timestamp)
    assertEquals(rate.timestamp.value, OffsetDateTime.parse("2024-08-04T13:00:00Z"))
  }

  test("Rate equality and hashCode include timestamp") {
    val t  = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    val r1 = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1.2), t)
    val r2 = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1.2), t)
    assertEquals(r1, r2)
    assertEquals(r1.hashCode, r2.hashCode)

    val r3 = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1.2), Timestamp(OffsetDateTime.parse("2025-01-01T01:00:00Z")))
    assert(r3 != r1)
  }
}
