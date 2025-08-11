package forex.domain.rates

import io.circe.parser.decode
import io.circe.syntax._
import munit.FunSuite
import java.time.OffsetDateTime
import scala.concurrent.duration._

class TimestampSpec extends FunSuite {

  test("Timestamp should store OffsetDateTime value correctly") {
    val now = OffsetDateTime.now
    val timestamp = Timestamp(now)
    assertEquals(timestamp.value, now)
  }

  test("Timestamp.now should create timestamp with UTC timezone") {
    val timestamp = Timestamp.now
    assertEquals(timestamp.value.getOffset, java.time.ZoneOffset.UTC)
  }

  test("Timestamp should be equal if values are equal") {
    val now = OffsetDateTime.now
    val timestamp1 = Timestamp(now)
    val timestamp2 = Timestamp(now)
    assertEquals(timestamp1, timestamp2)
  }

  test("Timestamp should have correct hashCode") {
    val now = OffsetDateTime.now
    val timestamp1 = Timestamp(now)
    val timestamp2 = Timestamp(now)
    assertEquals(timestamp1.hashCode(), timestamp2.hashCode())
  }

  test("isWithinTTL should return true for recent timestamp") {
    val recent = Timestamp(OffsetDateTime.now.minusSeconds(30))
    val ttl = 1.minute
    assert(Timestamp.isWithinTTL(recent, ttl))
  }

  test("isWithinTTL should return false for old timestamp") {
    val old = Timestamp(OffsetDateTime.now.minusMinutes(2))
    val ttl = 1.minute
    assert(!Timestamp.isWithinTTL(old, ttl))
  }

  test("olderTTL should return the earlier timestamp") {
    val earlier = Timestamp(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
    val later = Timestamp(OffsetDateTime.parse("2024-01-01T11:00:00Z"))
    val result = Timestamp.olderTTL(earlier, later)
    assertEquals(result, earlier)
  }

  test("olderTTL should return the earlier timestamp when order is reversed") {
    val earlier = Timestamp(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
    val later = Timestamp(OffsetDateTime.parse("2024-01-01T11:00:00Z"))
    val result = Timestamp.olderTTL(later, earlier)
    assertEquals(result, earlier)
  }

  test("Timestamp encoder should encode to ISO string") {
    val timestamp = Timestamp(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
    val json = timestamp.asJson
    assert(json.isString)
    assertEquals(json.asString.get, "2024-01-01T10:00:00.000000Z")
  }

  test("Timestamp decoder should decode from ISO string") {
    val json = "\"2024-01-01T10:00:00Z\""
    val result = decode[Timestamp](json)
    assert(result.isRight)
    assertEquals(result.toOption.get.value, OffsetDateTime.parse("2024-01-01T10:00:00Z"))
  }

  test("Timestamp decoder should fail for invalid format") {
    val json = "\"invalid-date\""
    val result = decode[Timestamp](json)
    assert(result.isLeft)
  }

  test("Timestamp decoder should fail for non-string JSON") {
    val json = "123"
    val result = decode[Timestamp](json)
    assert(result.isLeft)
  }
}
