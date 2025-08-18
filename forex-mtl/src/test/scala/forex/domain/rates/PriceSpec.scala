package forex.domain.rates

import io.circe.parser.decode
import io.circe.syntax._
import munit.FunSuite

class PriceSpec extends FunSuite {

  test("Price should handle BigDecimal values correctly") {
    val price = Price(BigDecimal("123.45"))
    assertEquals(price.value, BigDecimal("123.45"))
  }

  test("Price should handle Integer conversion") {
    val price = Price.fromInt(42).toOption.get
    assertEquals(price.value, BigDecimal(42))
  }

  test("Price should support JSON serialization") {
    val price = Price(BigDecimal("123.45"))
    val json  = price.asJson
    assert(json.isNumber)
  }

  test("Price should support JSON deserialization") {
    val json   = "123.45"
    val result = decode[Price](json)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, BigDecimal("123.45"))
  }

  test("Price should handle integer JSON") {
    val json   = "42"
    val result = decode[Price](json)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, BigDecimal(42))
  }

  test("Price should fail for invalid JSON") {
    val json   = "\"invalid\""
    val result = decode[Price](json)
    assert(result.isLeft)
  }

  test("Price.round should round correctly with HALF_UP") {
    val price1   = Price(BigDecimal(123.4567))
    val rounded1 = price1.round(3)
    assertEquals(rounded1.value, BigDecimal(123.457), "Price should round up to 3 decimal places")

    val price2   = Price(BigDecimal(123.4564))
    val rounded2 = price2.round(3)
    assertEquals(rounded2.value, BigDecimal(123.456), "Price should round down to 3 decimal places")

    val price3   = Price(BigDecimal(0.851234))
    val rounded3 = price3.round(5)
    assertEquals(rounded3.value, BigDecimal(0.85123), "Price should round to 5 decimal places")

    val price4   = Price(BigDecimal(0.851236))
    val rounded4 = price4.round(5)
    assertEquals(rounded4.value, BigDecimal(0.85124), "Price should round up to 5 decimal places")
  }

  test("Price.round should handle edge cases") {
    val smallPrice   = Price(BigDecimal(0.001))
    val roundedSmall = smallPrice.round(3)
    assertEquals(roundedSmall.value, BigDecimal(0.001), "Small price should maintain precision")

    val largePrice   = Price(BigDecimal(999999.999999))
    val roundedLarge = largePrice.round(5)
    assertEquals(roundedLarge.value, BigDecimal(1000000.00000), "Large price should round to 5 decimal places")

    val exactPrice   = Price(BigDecimal(100.000))
    val roundedExact = exactPrice.round(3)
    assertEquals(roundedExact.value, BigDecimal(100.000), "Exact price should maintain precision")

    val zeroPrice   = Price(BigDecimal(0))
    val roundedZero = zeroPrice.round(5)
    assertEquals(roundedZero.value, BigDecimal(0), "Zero price should remain zero")
  }

  test("Price.round should return new Price instance") {
    val originalPrice = Price(BigDecimal(123.4567))
    val roundedPrice  = originalPrice.round(3)

    assert(originalPrice ne roundedPrice, "round should return a new Price instance")
    assert(originalPrice.value != roundedPrice.value, "original and rounded prices should be different")

    assertEquals(originalPrice.value, BigDecimal(123.4567), "Original price should remain unchanged")
    assertEquals(roundedPrice.value, BigDecimal(123.457), "Rounded price should have correct value")
  }
}
