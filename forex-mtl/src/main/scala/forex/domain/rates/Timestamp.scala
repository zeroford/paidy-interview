package forex.domain.rates

import scala.concurrent.duration.FiniteDuration
import io.circe.{ Decoder, Encoder }
import cats.syntax.either._

import java.time.Instant
import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import java.time.temporal.ChronoUnit.MILLIS

final case class Timestamp(value: Instant) extends AnyVal
object Timestamp {
  private val fmt: DateTimeFormatter = new DateTimeFormatterBuilder().appendInstant(3).toFormatter()

  def withinTtl(timestamp: Timestamp, now: Instant, ttl: FiniteDuration): Boolean = {
    val tsVal      = timestamp.value
    val expireTime = tsVal.plusNanos(ttl.toNanos)
    !now.isBefore(tsVal) && !now.isAfter(expireTime)
  }

  def base(t1: Timestamp, t2: Timestamp): Timestamp = if (t1.value.isBefore(t2.value)) t1 else t2

  implicit val encoder: Encoder[Timestamp] =
    Encoder.encodeString.contramap(ts => fmt.format(ts.value.truncatedTo(MILLIS)))

  implicit val decoder: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    Either
      .catchNonFatal(Instant.from(fmt.parse(str.trim)))
      .map(Timestamp.apply)
      .leftMap(_ => "Invalid timestamp format")
  }
}
