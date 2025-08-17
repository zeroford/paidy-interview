package forex.domain.rates

import forex.domain.currency.Currency
import munit.FunSuite

import java.time.OffsetDateTime

class RateSpec extends FunSuite {

  test("Rate should handle different price types") {
    val priceVal          = BigDecimal("123.45")
    val priceInt: Integer = 42
    val rate1             = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(priceVal),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z"))
    )
    val rate2 = Rate(
      pair = Rate.Pair(Currency.EUR, Currency.GBP),
      price = Price.fromInt(priceInt),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T15:00:00Z"))
    )

    assertEquals(rate1.price.value, priceVal)
    assertEquals(rate2.price.value, BigDecimal(42))
  }

  test("Rate.Pair should handle currency pairs") {
    val pair = Rate.Pair(Currency.USD, Currency.CHF)
    assertEquals(pair.from, Currency.USD)
    assertEquals(pair.to, Currency.CHF)
  }

  test("fromPivotRate should handle USD as base correctly") {
    val usdPivot =
      PivotRate(Currency.USD, Price(1.0), Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z")))
    val eurPivot =
      PivotRate(Currency.EUR, Price(0.85), Timestamp(OffsetDateTime.parse("2024-08-04T14:00:00Z")))

    val rate = Rate.fromPivotRate(usdPivot, eurPivot)

    assertEquals(rate.pair, Rate.Pair(Currency.USD, Currency.EUR))
    assertEquals(rate.price.value, BigDecimal(0.85))
    assertEquals(rate.timestamp, eurPivot.timestamp)
  }

  test("fromPivotRate should handle USD as quote correctly") {
    val eurPivot =
      PivotRate(Currency.EUR, Price(0.85), Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z")))
    val usdPivot =
      PivotRate(Currency.USD, Price(1.0), Timestamp(OffsetDateTime.parse("2024-08-04T14:00:00Z")))

    val rate = Rate.fromPivotRate(eurPivot, usdPivot)

    assertEquals(rate.pair, Rate.Pair(Currency.EUR, Currency.USD))
    assertEquals(rate.price.value, BigDecimal(1.0) / BigDecimal(0.85))
    assertEquals(rate.timestamp, eurPivot.timestamp)
  }

  test("fromPivotRate should handle cross-rate correctly") {
    val eurPivot =
      PivotRate(Currency.EUR, Price(0.85), Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z")))
    val jpyPivot =
      PivotRate(Currency.JPY, Price(110.0), Timestamp(OffsetDateTime.parse("2024-08-04T14:00:00Z")))

    val rate = Rate.fromPivotRate(eurPivot, jpyPivot)

    assertEquals(rate.pair, Rate.Pair(Currency.EUR, Currency.JPY))
    assertEquals(rate.price.value, BigDecimal(110.0) / BigDecimal(0.85))
    assertEquals(rate.timestamp, eurPivot.timestamp)
  }

  test("fromPivotRate should use older timestamp for cross-rate when quote is older") {
    val eurPivot =
      PivotRate(Currency.EUR, Price(0.85), Timestamp(OffsetDateTime.parse("2024-08-04T14:00:00Z")))
    val jpyPivot =
      PivotRate(Currency.JPY, Price(110.0), Timestamp(OffsetDateTime.parse("2024-08-04T13:00:00Z")))

    val rate = Rate.fromPivotRate(eurPivot, jpyPivot)

    assertEquals(rate.pair, Rate.Pair(Currency.EUR, Currency.JPY))
    assertEquals(rate.price.value, BigDecimal(110.0) / BigDecimal(0.85))
    assertEquals(rate.timestamp, jpyPivot.timestamp)
  }
}
