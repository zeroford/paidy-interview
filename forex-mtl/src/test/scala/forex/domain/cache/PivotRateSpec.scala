package forex.domain.cache

import forex.domain.currency.Currency
import forex.domain.rates.{ PivotRate, Price, Timestamp }
import io.circe.parser.decode
import io.circe.syntax._
import munit.FunSuite

import java.time.{ OffsetDateTime, ZoneOffset }

class PivotRateSpec extends FunSuite {

  test("PivotRate should handle business operations correctly") {
    val currency  = Currency.USD
    val price     = Price(BigDecimal("1.23"))
    val timestamp = Timestamp(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
    val pivotRate = PivotRate(currency, price, timestamp)

    assertEquals(pivotRate.currency, currency)
    assertEquals(pivotRate.price, price)
    assertEquals(pivotRate.timestamp, timestamp)
  }

  test("default should create PivotRate with price 1 and current timestamp") {
    val currency  = Currency.EUR
    val pivotRate = PivotRate.default(currency)

    assertEquals(pivotRate.currency, currency)
    assertEquals(pivotRate.price.value, BigDecimal(1))
    assert(pivotRate.timestamp.value.getOffset == ZoneOffset.UTC)
  }

  test("fromResponse should create PivotRate from OneFrame response") {
    val currency  = Currency.USD
    val price     = BigDecimal("1.23")
    val timeStamp = "2024-01-01T10:00:00Z"
    val pivotRate = PivotRate.fromResponse(currency, price, timeStamp)

    assertEquals(pivotRate.currency, currency)
    assertEquals(pivotRate.price.value, price)
    assertEquals(pivotRate.timestamp.value, OffsetDateTime.parse(timeStamp))
  }

  test("PivotRate encoder should encode to JSON object") {
    val currency  = Currency.USD
    val price     = Price(BigDecimal("1.23"))
    val timestamp = Timestamp(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
    val pivotRate = PivotRate(currency, price, timestamp)
    val json      = pivotRate.asJson

    assert(json.isObject)
    assertEquals(json.hcursor.downField("currency").as[String].toOption.get, "USD")
    assertEquals(json.hcursor.downField("price").as[BigDecimal].toOption.get, BigDecimal("1.23"))
  }

  test("PivotRate decoder should decode from JSON object") {
    val json   = """{"currency":"EUR","price":1.23,"timestamp":"2024-01-01T10:00:00.000000Z"}"""
    val result = decode[PivotRate](json)

    if (result.isLeft) {
      println(s"PivotRate decode error: ${result.left.toOption.get}")
    }
    assert(result.isRight)
    val pivotRate = result.toOption.get
    assertEquals(pivotRate.currency, Currency.EUR)
    assertEquals(pivotRate.price.value, BigDecimal("1.23"))
  }

  test("PivotRate decoder should fail for invalid JSON") {
    val json   = """{"invalid":"data"}"""
    val result = decode[PivotRate](json)
    assert(result.isLeft)
  }
}
