package forex.clients.oneframe

import munit.FunSuite
import io.circe.parser.parse
import forex.clients.oneframe.Protocol.{ OneFrameApiError, OneFrameRate }
import java.time.Instant

class ProtocolSpec extends FunSuite {

  test("OneFrameRate decodes from JSON") {
    val json = """{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"2024-01-01T10:00:00Z"}"""
    val decoded = parse(json).flatMap(_.as[OneFrameRate])
    decoded match {
      case Right(rate) =>
        assertEquals(rate.from, "USD")
        assertEquals(rate.to, "EUR")
        assertEquals(rate.bid, BigDecimal(0.85))
        assertEquals(rate.ask, BigDecimal(0.86))
        assertEquals(rate.price, BigDecimal(0.855))
        assertEquals(rate.time_stamp, Instant.parse("2024-01-01T10:00:00Z"))
      case Left(e) => fail(s"decode failed: $e")
    }
  }

  test("OneFrameRate accepts RFC3339 with micros and without micros") {
    val j1 =
      """{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"2024-01-01T10:00:00.000000Z"}"""
    val j2 = """{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"2024-01-01T10:00:00Z"}"""
    val r1 = parse(j1).flatMap(_.as[OneFrameRate]).toOption.getOrElse(fail("decode 1 failed"))
    val r2 = parse(j2).flatMap(_.as[OneFrameRate]).toOption.getOrElse(fail("decode 2 failed"))
    assertEquals(r1.time_stamp, r2.time_stamp)
  }

  test("OneFrameRate rejects invalid timestamp format") {
    val json = """{"from":"USD","to":"EUR","bid":0.85,"ask":0.86,"price":0.855,"time_stamp":"invalid"}"""
    val res  = parse(json).flatMap(_.as[OneFrameRate])
    res match {
      case Left(err) => assert(err.toString.contains("Invalid provider time_stamp"))
      case Right(v)  => fail(s"expected failure, got: $v")
    }
  }

  test("OneFrameApiError decodes from JSON") {
    val json    = """{"error":"Quota reached"}"""
    val decoded = parse(json).flatMap(_.as[OneFrameApiError])
    decoded match {
      case Right(err) => assertEquals(err.error, "Quota reached")
      case Left(e)    => fail(s"decode failed: $e")
    }
  }

  test("OneFrameRate handles large numeric values") {
    val json =
      """{"from":"USD","to":"JPY","bid":999999.999999,"ask":999999.999999,"price":999999.999999,"time_stamp":"2024-01-01T10:00:00Z"}"""
    val decoded = parse(json).flatMap(_.as[OneFrameRate])
    decoded match {
      case Right(rate) =>
        assertEquals(rate.bid, BigDecimal(999999.999999))
        assertEquals(rate.ask, BigDecimal(999999.999999))
        assertEquals(rate.price, BigDecimal(999999.999999))
      case Left(e) => fail(s"decode failed: $e")
    }
  }
}
