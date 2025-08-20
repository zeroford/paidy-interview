package forex.domain.rates

import java.time.Instant
import scala.concurrent.duration._
import io.circe.syntax._
import munit.FunSuite

class TimestampSpec extends FunSuite {

  private val fixedInstant   = Instant.parse("2024-01-01T10:00:00Z")
  private val earlierInstant = Instant.parse("2024-01-01T09:00:00Z")

  test("Timestamp should preserve Instant value") {
    val timestamp = Timestamp(fixedInstant)
    assertEquals(timestamp.value, fixedInstant)
  }

  test("Timestamp.base should return the earlier timestamp") {
    val t1   = Timestamp(fixedInstant)
    val t2   = Timestamp(earlierInstant)
    val base = Timestamp.base(t1, t2)
    assertEquals(base.value, earlierInstant)
  }

  test("Timestamp.base should be commutative") {
    val t1    = Timestamp(fixedInstant)
    val t2    = Timestamp(earlierInstant)
    val base1 = Timestamp.base(t1, t2)
    val base2 = Timestamp.base(t2, t1)
    assertEquals(base1, base2)
  }

  test("Timestamp.base should be idempotent") {
    val t1    = Timestamp(fixedInstant)
    val base1 = Timestamp.base(t1, t1)
    assertEquals(base1, t1)
  }

  test("Timestamp.withinTtl should return true for valid timestamp") {
    val timestamp = Timestamp(fixedInstant)
    val now       = fixedInstant.plusSeconds(30)
    val ttl       = 1.minute

    assert(Timestamp.withinTtl(timestamp, now, ttl))
  }

  test("Timestamp.withinTtl should return false for expired timestamp") {
    val timestamp = Timestamp(fixedInstant)
    val now       = fixedInstant.plusSeconds(120)
    val ttl       = 1.minute

    assert(!Timestamp.withinTtl(timestamp, now, ttl))
  }

  test("Timestamp.withinTtl should handle zero TTL") {
    val timestamp = Timestamp(fixedInstant)
    val now       = fixedInstant
    val ttl       = 0.seconds

    assert(Timestamp.withinTtl(timestamp, now, ttl))
  }

  test("Timestamp should support round-trip JSON serialization") {
    val timestamp = Timestamp(fixedInstant)
    val json      = timestamp.asJson
    val decoded   = json.as[Timestamp]
    assert(decoded.isRight)
    assertEquals(decoded.toOption.get, timestamp)
  }

  test("Timestamp should handle min Instant") {
    val timestamp = Timestamp(Instant.MIN)
    assertEquals(timestamp.value, Instant.MIN)
  }

  test("Timestamp should handle max Instant") {
    val timestamp = Timestamp(Instant.MAX)
    assertEquals(timestamp.value, Instant.MAX)
  }

  test("Timestamp.withinTtl should handle exact expiration") {
    val timestamp = Timestamp(fixedInstant)
    val now       = fixedInstant.plusSeconds(60)
    val ttl       = 1.minute

    assert(Timestamp.withinTtl(timestamp, now, ttl))
  }

  test("Timestamp.withinTtl should handle negative TTL") {
    val timestamp = Timestamp(fixedInstant)
    val now       = fixedInstant.plusSeconds(30)
    val ttl       = (-1).minute

    assert(!Timestamp.withinTtl(timestamp, now, ttl))
  }

  test("Timestamp decoder should fail for invalid JSON") {
    val invalidJson = io.circe.parser.parse("""{"invalid":"data"}""").toOption.get
    val result      = io.circe.Decoder[Timestamp].decodeJson(invalidJson)
    assert(result.isLeft)
  }
}
