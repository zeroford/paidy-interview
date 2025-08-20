package forex.domain.rates

import java.time.Instant
import munit.FunSuite
import forex.domain.currency.Currency

class RateSpec extends FunSuite {

  private val fixedInstant1 = Instant.parse("2024-08-04T13:00:00Z")
  private val fixedInstant2 = Instant.parse("2024-08-04T15:00:00Z")

  test("Rate.Pair should preserve currency order") {
    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    assertEquals(pair.from, Currency.USD)
    assertEquals(pair.to, Currency.EUR)
  }

  test("Rate.Pair.fractionalPip should return 3 for JPY pairs") {
    val jpyFromPair = Rate.Pair(Currency.JPY, Currency.USD)
    val jpyToPair   = Rate.Pair(Currency.USD, Currency.JPY)

    assertEquals(jpyFromPair.fractionalPip, 3)
    assertEquals(jpyToPair.fractionalPip, 3)
  }

  test("Rate.Pair.fractionalPip should return 5 for non-JPY pairs") {
    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    assertEquals(pair.fractionalPip, 5)
  }

  test("Rate.fromPivotRate should handle USD as base correctly") {
    val base  = PivotRate(Currency.USD, Price(BigDecimal(1)), Timestamp(fixedInstant1))
    val quote = PivotRate(Currency.EUR, Price(BigDecimal(0.85)), Timestamp(fixedInstant2))

    val rate = Rate.fromPivotRate(base, quote)

    assertEquals(rate.pair.from, Currency.USD)
    assertEquals(rate.pair.to, Currency.EUR)
    assertEquals(rate.price.value, BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant2))
  }

  test("Rate.fromPivotRate should handle USD as quote correctly") {
    val base  = PivotRate(Currency.EUR, Price(BigDecimal(0.85)), Timestamp(fixedInstant1))
    val quote = PivotRate(Currency.USD, Price(BigDecimal(1)), Timestamp(fixedInstant2))

    val rate = Rate.fromPivotRate(base, quote)

    assertEquals(rate.pair.from, Currency.EUR)
    assertEquals(rate.pair.to, Currency.USD)
    assertEquals(rate.price.value, BigDecimal(1) / BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant1))
  }

  test("Rate.fromPivotRate should handle cross-rate correctly") {
    val base  = PivotRate(Currency.EUR, Price(BigDecimal(0.85)), Timestamp(fixedInstant1))
    val quote = PivotRate(Currency.GBP, Price(BigDecimal(0.75)), Timestamp(fixedInstant2))

    val rate = Rate.fromPivotRate(base, quote)

    assertEquals(rate.pair.from, Currency.EUR)
    assertEquals(rate.pair.to, Currency.GBP)
    assertEquals(rate.price.value, BigDecimal(0.75) / BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant1))
  }

  test("Rate.fromPivotRate should handle timestamp ordering edge case") {
    val base  = PivotRate(Currency.EUR, Price(BigDecimal(0.85)), Timestamp(fixedInstant2))
    val quote = PivotRate(Currency.JPY, Price(BigDecimal(110.0)), Timestamp(fixedInstant1))

    val rate = Rate.fromPivotRate(base, quote)

    assertEquals(rate.pair.from, Currency.EUR)
    assertEquals(rate.pair.to, Currency.JPY)
    assertEquals(rate.price.value, BigDecimal(110.0) / BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant1)) // Should use older timestamp
  }

  test("Rate should handle basic construction") {
    val rate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.EUR),
      price = Price(BigDecimal(0.85)),
      timestamp = Timestamp(fixedInstant1)
    )

    assertEquals(rate.pair.from, Currency.USD)
    assertEquals(rate.pair.to, Currency.EUR)
    assertEquals(rate.price.value, BigDecimal(0.85))
    assertEquals(rate.timestamp, Timestamp(fixedInstant1))
  }
}
