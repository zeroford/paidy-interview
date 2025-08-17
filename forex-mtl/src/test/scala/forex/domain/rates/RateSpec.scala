package forex.domain.rates

import java.time.Instant

import munit.FunSuite

import forex.domain.currency.Currency

class RateSpec extends FunSuite {

  private val fixedInstant1 = Instant.parse("2024-08-04T13:00:00Z")
  private val fixedInstant2 = Instant.parse("2024-08-04T15:00:00Z")

  test("Rate should handle different price types") {
    val rate1 = Rate(
      pair = Rate.Pair(Currency.USD, Currency.EUR),
      price = Price(BigDecimal(0.85)),
      timestamp = Timestamp(fixedInstant1)
    )
    val rate2 = Rate(
      pair = Rate.Pair(Currency.EUR, Currency.GBP),
      price = Price(BigDecimal(0.90)),
      timestamp = Timestamp(fixedInstant2)
    )

    assertEquals(rate1.pair.from, Currency.USD)
    assertEquals(rate1.pair.to, Currency.EUR)
    assertEquals(rate1.price.value, BigDecimal(0.85))
    assertEquals(rate1.timestamp, Timestamp(fixedInstant1))

    assertEquals(rate2.pair.from, Currency.EUR)
    assertEquals(rate2.pair.to, Currency.GBP)
    assertEquals(rate2.price.value, BigDecimal(0.90))
    assertEquals(rate2.timestamp, Timestamp(fixedInstant2))
  }

  test("Rate.Pair should handle currency pairs") {
    val pair = Rate.Pair(Currency.USD, Currency.JPY)
    assertEquals(pair.from, Currency.USD)
    assertEquals(pair.to, Currency.JPY)
  }

  test("fromPivotRate should handle USD as base correctly") {
    val base  = PivotRate(Currency.USD, Price(1.0), Timestamp(fixedInstant1))
    val quote = PivotRate(Currency.EUR, Price(0.85), Timestamp(fixedInstant2))

    val rate = Rate.fromPivotRate(base, quote)

    assertEquals(rate.pair.from, Currency.USD)
    assertEquals(rate.pair.to, Currency.EUR)
    assertEquals(rate.price.value, BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant2))
  }

  test("fromPivotRate should handle USD as quote correctly") {
    val base  = PivotRate(Currency.EUR, Price(0.85), Timestamp(fixedInstant1))
    val quote = PivotRate(Currency.USD, Price(1.0), Timestamp(fixedInstant2))

    val rate = Rate.fromPivotRate(base, quote)

    assertEquals(rate.pair.from, Currency.EUR)
    assertEquals(rate.pair.to, Currency.USD)
    assertEquals(rate.price.value, BigDecimal(1.0) / BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant1))
  }

  test("fromPivotRate should handle cross-rate correctly") {
    val base  = PivotRate(Currency.EUR, Price(0.85), Timestamp(fixedInstant1))
    val quote = PivotRate(Currency.JPY, Price(110.0), Timestamp(fixedInstant2))

    val rate = Rate.fromPivotRate(base, quote)

    assertEquals(rate.pair.from, Currency.EUR)
    assertEquals(rate.pair.to, Currency.JPY)
    assertEquals(rate.price.value, BigDecimal(110.0) / BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant1))
  }

  test("fromPivotRate should use older timestamp for cross-rate when quote is older") {
    val base  = PivotRate(Currency.EUR, Price(0.85), Timestamp(fixedInstant2))
    val quote = PivotRate(Currency.JPY, Price(110.0), Timestamp(fixedInstant1))

    val rate = Rate.fromPivotRate(base, quote)

    assertEquals(rate.pair.from, Currency.EUR)
    assertEquals(rate.pair.to, Currency.JPY)
    assertEquals(rate.price.value, BigDecimal(110.0) / BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant1))
  }
}
