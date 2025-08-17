package forex.domain.rates

import java.time.Instant
import scala.concurrent.duration._

import io.circe.{ Decoder, Encoder }
import io.circe.parser.parse
import munit.FunSuite

class TimestampSpec extends FunSuite {

  private val fixedInstant   = Instant.parse("2024-01-01T10:00:00Z")
  private val laterInstant   = Instant.parse("2024-01-01T11:00:00Z")
  private val earlierInstant = Instant.parse("2024-01-01T09:00:00Z")

  test("Timestamp should create from Instant correctly") {
    val timestamp = Timestamp(fixedInstant)
    assertEquals(timestamp.value, fixedInstant)
  }

  test("Timestamp should be immutable") {
    val timestamp = Timestamp(fixedInstant)
    val modified  = timestamp.copy(value = laterInstant)

    assertEquals(timestamp.value, fixedInstant)
    assertEquals(modified.value, laterInstant)
  }

  test("withinTtl should work correctly with pure functions") {
    val ttl = 1.minute
    val now = fixedInstant

    val recent = Timestamp(now.minusSeconds(30))
    val old    = Timestamp(now.minusSeconds(120))
    val future = Timestamp(now.plusSeconds(30))

    val recentValid = Timestamp.withinTtl(recent, now, ttl)
    val oldValid    = Timestamp.withinTtl(old, now, ttl)
    val futureValid = Timestamp.withinTtl(future, now, ttl)

    assert(recentValid, "Recent timestamp should be within TTL")
    assert(!oldValid, "Old timestamp should be expired")
    assert(!futureValid, "Future timestamp should not be within TTL")
  }

  test("base should return earlier timestamp (pure function)") {
    val earlier = Timestamp(earlierInstant)
    val later   = Timestamp(laterInstant)

    val result1 = Timestamp.base(earlier, later)
    val result2 = Timestamp.base(later, earlier)

    assertEquals(result1, earlier, "Should return earlier timestamp")
    assertEquals(result2, earlier, "Should return earlier timestamp regardless of order")
  }

  test("base should handle identical timestamps") {
    val same   = Timestamp(fixedInstant)
    val result = Timestamp.base(same, same)
    assertEquals(result, same)
  }

  test("Timestamp should support JSON serialization (pure)") {
    val timestamp = Timestamp(fixedInstant)
    val json      = Encoder[Timestamp].apply(timestamp)

    assert(json.isString, "Should serialize to string")
    assert(json.asString.isDefined, "Should be a valid string")
  }

  test("Timestamp should support JSON deserialization (pure)") {
    val timestamp = Timestamp(fixedInstant)
    val json      = Encoder[Timestamp].apply(timestamp)
    val result    = Decoder[Timestamp].decodeJson(json)

    assert(result.isRight, "Should decode successfully")
    assertEquals(result.toOption.get, timestamp, "Should decode to same timestamp")
  }

  test("Timestamp should fail for invalid JSON (pure error handling)") {
    val invalidJson = parse("\"invalid-timestamp\"").toOption.get
    val result      = Decoder[Timestamp].decodeJson(invalidJson)

    assert(result.isLeft, "Should fail for invalid timestamp")
    assert(
      result.left.toOption.get.message.contains("Invalid timestamp format"),
      "Should have meaningful error message"
    )
  }

  test("Timestamp should handle edge cases") {
    val epoch     = Timestamp(Instant.EPOCH)
    val farFuture = Timestamp(Instant.parse("9999-12-31T23:59:59Z"))

    assertEquals(epoch.value, Instant.EPOCH)
    assert(farFuture.value.isAfter(Instant.now()))
  }

  test("withinTtl should handle zero TTL") {
    val timestamp = Timestamp(fixedInstant)
    val now       = fixedInstant
    val zeroTtl   = 0.seconds

    val result = Timestamp.withinTtl(timestamp, now, zeroTtl)
    assert(result, "Exact timestamp should be within zero TTL")
  }

  test("withinTtl should handle very large TTL") {
    val timestamp = Timestamp(fixedInstant)
    val now       = fixedInstant.plusSeconds(1000)
    val largeTtl  = 1.hour

    val result = Timestamp.withinTtl(timestamp, now, largeTtl)
    assert(result, "Should be within large TTL")
  }
}
