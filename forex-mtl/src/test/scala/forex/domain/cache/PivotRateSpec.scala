package forex.domain.cache

import java.time.Instant

import io.circe.{ Decoder, Encoder }
import munit.FunSuite

import forex.domain.currency.Currency
import forex.domain.rates.{ PivotRate, Price, Timestamp }

class PivotRateSpec extends FunSuite {

  private val fixedInstant = Instant.parse("2024-01-01T10:00:00Z")
  private val usdCurrency  = Currency.USD
  private val eurCurrency  = Currency.EUR
  private val testPrice    = Price(BigDecimal(1.5))

  test("PivotRate should handle business operations correctly") {
    val pivotRate = PivotRate(usdCurrency, testPrice, Timestamp(fixedInstant))

    assertEquals(pivotRate.currency, usdCurrency)
    assertEquals(pivotRate.price, testPrice)
    assertEquals(pivotRate.timestamp, Timestamp(fixedInstant))
  }

  test("PivotRate should be immutable") {
    val original = PivotRate(usdCurrency, testPrice, Timestamp(fixedInstant))
    val modified = original.copy(currency = eurCurrency)

    assertEquals(original.currency, usdCurrency)
    assertEquals(modified.currency, eurCurrency)
  }

  test("default should create PivotRate with price 1 and current timestamp") {
    val now       = fixedInstant
    val pivotRate = PivotRate.default(usdCurrency, now)

    assertEquals(pivotRate.currency, usdCurrency)
    assertEquals(pivotRate.price, Price(BigDecimal(1)))
    assertEquals(pivotRate.timestamp, Timestamp(now))
  }

  test("fromResponse should create PivotRate from Instant") {
    val price     = BigDecimal(1.25)
    val instant   = fixedInstant
    val pivotRate = PivotRate.fromResponse(usdCurrency, price, instant)

    assertEquals(pivotRate.currency, usdCurrency)
    assertEquals(pivotRate.price, Price(price))
    assertEquals(pivotRate.timestamp, Timestamp(instant))
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

  test("PivotRate encoder should encode to JSON object") {
    val pivotRate = PivotRate(usdCurrency, testPrice, Timestamp(fixedInstant))
    val json      = Encoder[PivotRate].apply(pivotRate)

    assert(json.isObject, "Should encode to JSON object")
    assert(json.hcursor.get[String]("currency").toOption.contains("USD"), "Should encode currency")
    assert(json.hcursor.get[BigDecimal]("price").toOption.contains(BigDecimal(1.5)), "Should encode price")
  }

  test("PivotRate decoder should decode from JSON object") {
    val pivotRate = PivotRate(usdCurrency, testPrice, Timestamp(fixedInstant))
    val json      = Encoder[PivotRate].apply(pivotRate)
    val result    = Decoder[PivotRate].decodeJson(json)

    assert(result.isRight, "Should decode successfully")
    assertEquals(result.toOption.get, pivotRate, "Should decode to same PivotRate")
  }

  test("PivotRate decoder should fail for invalid JSON") {
    val invalidJson = io.circe.parser.parse("""{"invalid":"data"}""").toOption.get
    val result      = Decoder[PivotRate].decodeJson(invalidJson)

    assert(result.isLeft, "Should fail for invalid JSON")
  }

  test("PivotRate should handle edge case prices") {
    val zeroPrice  = Price(BigDecimal(0))
    val largePrice = Price(BigDecimal(999999.99))

    val zeroRate  = PivotRate.fromResponse(usdCurrency, BigDecimal(0), fixedInstant)
    val largeRate = PivotRate.fromResponse(usdCurrency, BigDecimal(999999.99), fixedInstant)

    assertEquals(zeroRate.price, zeroPrice)
    assertEquals(largeRate.price, largePrice)
  }

  test("PivotRate should be consistent across multiple calls") {
    val rate1 = PivotRate.default(usdCurrency, fixedInstant)
    val rate2 = PivotRate.default(usdCurrency, fixedInstant)

    assertEquals(rate1, rate2, "Same inputs should produce same output")
  }

  test("PivotRate should handle different timestamps") {
    val instant1 = Instant.parse("2024-01-01T10:00:00Z")
    val instant2 = Instant.parse("2024-12-31T23:59:59Z")

    val rate1 = PivotRate.fromResponse(usdCurrency, BigDecimal(1.0), instant1)
    val rate2 = PivotRate.fromResponse(usdCurrency, BigDecimal(1.0), instant2)

    assert(rate1.timestamp.value.isBefore(rate2.timestamp.value))
  }
}
