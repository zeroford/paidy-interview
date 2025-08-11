package forex.domain.rates

import io.circe.parser.decode
import io.circe.syntax._
import munit.FunSuite

class PriceSpec extends FunSuite {

  test("Price should store BigDecimal value correctly") {
    val price = Price(BigDecimal("123.45"))
    assertEquals(price.value, BigDecimal("123.45"))
  }

  test("Price.apply(Integer) should convert to BigDecimal") {
    val price = Price(42: Integer)
    assertEquals(price.value, BigDecimal(42))
  }

  test("Price should be equal if values are equal") {
    val price1 = Price(BigDecimal("123.45"))
    val price2 = Price(BigDecimal("123.45"))
    assertEquals(price1, price2)
  }

  test("Price should have correct hashCode") {
    val price1 = Price(BigDecimal("123.45"))
    val price2 = Price(BigDecimal("123.45"))
    assertEquals(price1.hashCode(), price2.hashCode())
  }

  test("Price encoder should encode to JSON number") {
    val price = Price(BigDecimal("123.45"))
    val json  = price.asJson
    assertEquals(json.asString, None)
    assert(json.isNumber)
  }

  test("Price decoder should decode from JSON number") {
    val json   = "123.45"
    val result = decode[Price](json)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, BigDecimal("123.45"))
  }

  test("Price decoder should handle integer JSON") {
    val json   = "42"
    val result = decode[Price](json)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, BigDecimal(42))
  }

  test("Price decoder should fail for invalid JSON") {
    val json   = "\"invalid\""
    val result = decode[Price](json)
    assert(result.isLeft)
  }
}
