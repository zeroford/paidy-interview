package forex.domain.rates

import io.circe.parser.decode
import io.circe.syntax._
import munit.FunSuite
import java.time.OffsetDateTime
import scala.concurrent.duration._

class TimestampSpec extends FunSuite {

  test("Timestamp should handle OffsetDateTime correctly") {
    val now       = OffsetDateTime.now
    val timestamp = Timestamp(now)
    assertEquals(timestamp.value, now)
  }

  test("Timestamp.now should create UTC timestamp") {
    val timestamp = Timestamp.now
    assertEquals(timestamp.value.getOffset, java.time.ZoneOffset.UTC)
  }

  test("isWithinTTL should work correctly") {
    val recent = Timestamp(OffsetDateTime.now.minusSeconds(30))
    val old    = Timestamp(OffsetDateTime.now.minusMinutes(2))
    val ttl    = 1.minute

    assert(Timestamp.isWithinTTL(recent, ttl))
    assert(!Timestamp.isWithinTTL(old, ttl))
  }

  test("olderTTL should return earlier timestamp") {
    val earlier = Timestamp(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
    val later   = Timestamp(OffsetDateTime.parse("2024-01-01T11:00:00Z"))

    assertEquals(Timestamp.olderTTL(earlier, later), earlier)
    assertEquals(Timestamp.olderTTL(later, earlier), earlier)
  }

  test("Timestamp should support JSON serialization") {
    val timestamp = Timestamp(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
    val json      = timestamp.asJson
    assert(json.isString)
    assertEquals(json.asString.get, "2024-01-01T10:00:00.000000Z")
  }

  test("Timestamp should support JSON deserialization") {
    val json   = "\"2024-01-01T10:00:00Z\""
    val result = decode[Timestamp](json)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, OffsetDateTime.parse("2024-01-01T10:00:00Z"))
  }

  test("Timestamp should fail for invalid JSON") {
    val json   = "\"invalid-date\""
    val result = decode[Timestamp](json)
    assert(result.isLeft)
  }
}
