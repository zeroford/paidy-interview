package forex.domain.cache

import java.time.Instant
import io.circe.{ Decoder, Encoder }
import munit.FunSuite
import forex.domain.currency.Currency
import forex.domain.rates.{ PivotRate, Price, Timestamp }

class PivotRateSpec extends FunSuite {

  private val fixedInstant = Instant.parse("2024-01-01T10:00:00Z")
  private val usdCurrency  = Currency.USD

  test("PivotRate should preserve constructor values") {
    val currency  = Currency.EUR
    val price     = Price(BigDecimal(1.5))
    val timestamp = Timestamp(fixedInstant)

    val pivotRate = PivotRate(currency, price, timestamp)
    assertEquals(pivotRate.currency, currency)
    assertEquals(pivotRate.price, price)
    assertEquals(pivotRate.timestamp, timestamp)
  }

  test("PivotRate.default should create with price 1 and current timestamp") {
    val currency  = Currency.EUR
    val now       = fixedInstant
    val pivotRate = PivotRate.default(currency, now)

    assertEquals(pivotRate.currency, currency)
    assertEquals(pivotRate.price, Price(BigDecimal(1)))
    assertEquals(pivotRate.timestamp, Timestamp(now))
  }

  test("PivotRate.fromResponse should create from parameters") {
    val currency = Currency.GBP
    val price    = BigDecimal(0.75)
    val instant  = fixedInstant

    val pivotRate = PivotRate.fromResponse(currency, price, instant)
    assertEquals(pivotRate.currency, currency)
    assertEquals(pivotRate.price, Price(price))
    assertEquals(pivotRate.timestamp, Timestamp(instant))
  }

  test("PivotRate should be consistent across multiple calls") {
    val currency  = Currency.JPY
    val price     = Price(BigDecimal(110.0))
    val timestamp = Timestamp(fixedInstant)

    val rate1 = PivotRate(currency, price, timestamp)
    val rate2 = PivotRate(currency, price, timestamp)
    assertEquals(rate1, rate2)
  }

  test("PivotRate should support round-trip JSON serialization") {
    val pivotRate = PivotRate(Currency.EUR, Price(BigDecimal(1.5)), Timestamp(fixedInstant))
    val json      = Encoder[PivotRate].apply(pivotRate)
    val result    = Decoder[PivotRate].decodeJson(json)
    assert(result.isRight)
    assertEquals(result.toOption.get, pivotRate)
  }

  test("PivotRate should handle different currencies") {
    val currencies = List(Currency.USD, Currency.EUR, Currency.GBP, Currency.JPY)

    currencies.foreach { currency =>
      val pivotRate = PivotRate.default(currency, fixedInstant)
      assertEquals(pivotRate.currency, currency)
      assertEquals(pivotRate.price, Price(BigDecimal(1)))
    }
  }

  test("PivotRate should handle different prices") {
    val prices = List(BigDecimal(0.5), BigDecimal(1.0), BigDecimal(2.5), BigDecimal(100.0))

    prices.foreach { price =>
      val pivotRate = PivotRate.fromResponse(usdCurrency, price, fixedInstant)
      assertEquals(pivotRate.price, Price(price))
    }
  }

  test("PivotRate should handle edge case prices") {
    val zeroPrice  = Price(BigDecimal(0))
    val largePrice = Price(BigDecimal(999999.99))

    val zeroRate  = PivotRate.fromResponse(usdCurrency, BigDecimal(0), fixedInstant)
    val largeRate = PivotRate.fromResponse(usdCurrency, BigDecimal(999999.99), fixedInstant)

    assertEquals(zeroRate.price, zeroPrice)
    assertEquals(largeRate.price, largePrice)
  }

  test("PivotRate should handle different timestamps") {
    val instant1 = Instant.parse("2024-01-01T10:00:00Z")
    val instant2 = Instant.parse("2024-12-31T23:59:59Z")

    val rate1 = PivotRate.fromResponse(usdCurrency, BigDecimal(1.0), instant1)
    val rate2 = PivotRate.fromResponse(usdCurrency, BigDecimal(1.0), instant2)

    assert(rate1.timestamp.value.isBefore(rate2.timestamp.value))
  }

  test("PivotRate decoder should fail for invalid JSON") {
    val invalidJson = io.circe.parser.parse("""{"invalid":"data"}""").toOption.get
    val result      = Decoder[PivotRate].decodeJson(invalidJson)

    assert(result.isLeft, "Should fail for invalid JSON")
  }
}
