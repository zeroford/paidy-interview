package forex.domain.rates

import java.time.OffsetDateTime
import io.circe.{ Decoder, Encoder }

import scala.concurrent.duration.FiniteDuration

final case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp = Timestamp(OffsetDateTime.now)

  def isWithinTTL(timestamp: Timestamp, ttl: FiniteDuration): Boolean = {
    val now  = OffsetDateTime.now
    val diff = java.time.Duration.between(timestamp.value, now)
    diff.compareTo(java.time.Duration.ofSeconds(ttl.toSeconds)) < 0
  }

  def olderTTL(t1: Timestamp, t2: Timestamp): Timestamp = if (t1.value.isBefore(t2.value)) t1 else t2

  implicit val encoder: Encoder[Timestamp] = Encoder.encodeString.contramap(_.value.toString)
  implicit val decoder: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    try
      Right(Timestamp(OffsetDateTime.parse(str)))
    catch {
      case _: Exception => Left("Invalid timestamp format")
    }
  }
}
