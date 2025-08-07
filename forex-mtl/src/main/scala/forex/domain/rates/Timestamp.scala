package forex.domain.rates

import java.time.OffsetDateTime
import io.circe.{ Decoder, Encoder }

final case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val encoder: Encoder[Timestamp] = Encoder.encodeString.contramap(_.value.toString)
  implicit val decoder: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    try
      Right(Timestamp(OffsetDateTime.parse(str)))
    catch {
      case _: Exception => Left("Invalid timestamp format")
    }
  }
}
