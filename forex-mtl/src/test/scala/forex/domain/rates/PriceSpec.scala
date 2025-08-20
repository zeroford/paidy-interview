package forex.domain.rates

import io.circe.syntax._
import munit.FunSuite

class PriceSpec extends FunSuite {

  test("Price should preserve BigDecimal value") {
    val value = BigDecimal(123.45)
    val price = Price(value)
    assertEquals(price.value, value)
  }

  test("Price.fromInt should work for positive integers") {
    val result = Price.fromInt(100)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, BigDecimal(100))
  }

  test("Price.fromInt should fail for negative integers") {
    val result = Price.fromInt(-100)
    assert(result.isLeft)
  }

  test("Price.round should maintain value within scale") {
    val price   = Price(BigDecimal(123.456789))
    val rounded = price.round(2)
    assertEquals(rounded.value.scale, 2)
    assertEquals(rounded.value, BigDecimal(123.46))
  }

  test("Price.round should be idempotent") {
    val price    = Price(BigDecimal(123.456789))
    val rounded1 = price.round(2)
    val rounded2 = rounded1.round(2)
    assertEquals(rounded1, rounded2)
  }

  test("Price should support round-trip JSON serialization") {
    val price   = Price(BigDecimal(123.45))
    val json    = price.asJson
    val decoded = json.as[Price]
    assert(decoded.isRight)
    assertEquals(decoded.toOption.get, price)
  }

  test("Price should handle equality correctly") {
    val price1 = Price(BigDecimal(123.45))
    val price2 = Price(BigDecimal(123.45))
    val price3 = Price(BigDecimal(678.90))

    assertEquals(price1, price2)
    assert(price1 != price3)
  }

  test("Price should handle zero") {
    val result = Price.fromInt(0)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, BigDecimal(0))
  }

  test("Price should handle boundary rounding") {
    val price   = Price(BigDecimal(123.445))
    val rounded = price.round(2)
    assertEquals(rounded.value, BigDecimal(123.45))
  }

  test("Price should fail for invalid JSON") {
    val json   = "invalid".asJson
    val result = json.as[Price]
    assert(result.isLeft)
  }

  test("Price should handle fromInt with maximum integer") {
    val result = Price.fromInt(Int.MaxValue)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, BigDecimal(Int.MaxValue))
  }

  test("Price should handle fromInt with minimum integer") {
    val result = Price.fromInt(Int.MinValue)
    assert(result.isLeft)
  }
}
