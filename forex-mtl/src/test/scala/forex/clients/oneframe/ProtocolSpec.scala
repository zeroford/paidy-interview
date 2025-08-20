package forex.clients.oneframe

import munit.FunSuite
import io.circe.parser.parse
import forex.clients.oneframe.Protocol.{ OneFrameApiError, OneFrameRate }

class ProtocolSpec extends FunSuite {

  test("OneFrameRate should support JSON deserialization") {
    val json = """{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"2024-01-01T10:00:00Z"}"""
    val decoded = parse(json).flatMap(_.as[OneFrameRate])

    assert(decoded.isRight)
    val rate = decoded.toOption.get
    assertEquals(rate.from, "USD")
    assertEquals(rate.to, "EUR")
    assertEquals(rate.bid, BigDecimal(0.85))
    assertEquals(rate.ask, BigDecimal(0.86))
    assertEquals(rate.price, BigDecimal(0.855))
  }

  test("OneFrameRate should handle different timestamp formats") {
    val json1 = """{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"2024-01-01T10:00:00Z"}"""
    val json2 =
      """{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"2024-01-01T10:00:00+00:00"}"""

    val result1 = parse(json1).flatMap(_.as[OneFrameRate])
    val result2 = parse(json2).flatMap(_.as[OneFrameRate])

    assert(result1.isRight)
    assert(result2.isRight)
    assertEquals(result1.toOption.get.time_stamp, result2.toOption.get.time_stamp)
  }

  test("OneFrameRate should handle invalid timestamp format") {
    val json = """{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"invalid-timestamp"}"""

    val result = parse(json).flatMap(_.as[OneFrameRate])

    assert(result.isLeft)
    assert(result.left.toOption.get.toString.contains("Invalid provider time_stamp"))
  }

  test("OneFrameRatesResponse should support JSON deserialization") {
    val json =
      """[{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"2024-01-01T10:00:00Z"},{"from":"EUR","to":"GBP","bid":0.75,"ask":0.76,"price":0.755,"time_stamp":"2024-01-01T11:00:00Z"}]"""
    val decoded = parse(json).flatMap(_.as[List[OneFrameRate]])

    assert(decoded.isRight)
    val rates = decoded.toOption.get
    assertEquals(rates.length, 2)
    assertEquals(rates(0).from, "USD")
    assertEquals(rates(1).from, "EUR")
  }

  test("OneFrameApiError should support JSON deserialization") {
    val json    = """{"error":"Rate limit exceeded"}"""
    val decoded = parse(json).flatMap(_.as[OneFrameApiError])

    assert(decoded.isRight)
    assertEquals(decoded.toOption.get.error, "Rate limit exceeded")
  }

  test("OneFrameApiError should handle different error messages") {
    val errorMessages = List(
      "Rate limit exceeded",
      "Invalid currency pair",
      "Service unavailable",
      "Authentication failed"
    )

    errorMessages.foreach { message =>
      val json    = s"""{"error":"$message"}"""
      val decoded = parse(json).flatMap(_.as[OneFrameApiError])

      assert(decoded.isRight)
      assertEquals(decoded.toOption.get.error, message)
    }
  }

  test("OneFrameRate should handle edge case values") {
    val json    = """{"from":"","to":"","bid":0,"ask":0,"price":0,"time_stamp":"1970-01-01T00:00:00Z"}"""
    val decoded = parse(json).flatMap(_.as[OneFrameRate])

    assert(decoded.isRight)
    val rate = decoded.toOption.get
    assertEquals(rate.from, "")
    assertEquals(rate.to, "")
    assertEquals(rate.bid, BigDecimal(0))
    assertEquals(rate.ask, BigDecimal(0))
    assertEquals(rate.price, BigDecimal(0))
  }

  test("OneFrameRate should handle large numbers") {
    val json =
      """{"from":"USD","to":"JPY","bid":999999.999999,"ask":999999.999999,"price":999999.999999,"time_stamp":"2024-01-01T10:00:00Z"}"""
    val decoded = parse(json).flatMap(_.as[OneFrameRate])

    assert(decoded.isRight)
    val rate = decoded.toOption.get
    assertEquals(rate.bid, BigDecimal(999999.999999))
    assertEquals(rate.ask, BigDecimal(999999.999999))
    assertEquals(rate.price, BigDecimal(999999.999999))
  }
}
