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
}
