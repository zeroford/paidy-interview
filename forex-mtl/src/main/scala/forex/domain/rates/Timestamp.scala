package forex.domain.rates

import java.time.{ Duration, Instant, OffsetDateTime, ZoneOffset }
import io.circe.{ Decoder, Encoder }
import cats.effect.Clock
import cats.syntax.either._
import cats.syntax.functor._
import cats.Functor

import java.time.format.DateTimeFormatter
import scala.concurrent.duration.FiniteDuration

final case class Timestamp(value: OffsetDateTime) extends AnyVal
object Timestamp {

  def now[F[_]: Clock: Functor]: F[Timestamp] =
    Clock[F].realTime.map { d =>
      Timestamp(OffsetDateTime.ofInstant(Instant.ofEpochMilli(d.toMillis), ZoneOffset.UTC))
    }

  def isWithinTTL[F[_]: Clock: Functor](timestamp: Timestamp, ttl: FiniteDuration): F[Boolean] =
    now[F].map { currentTime =>
      val diff = Duration.between(timestamp.value, currentTime.value)
      diff.compareTo(Duration.ofSeconds(ttl.toSeconds)) < 0
    }

  def older(t1: Timestamp, t2: Timestamp): Timestamp =
    if (t1.value.isBefore(t2.value)) t1 else t2

  private val fmt: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

  implicit val encoder: Encoder[Timestamp] =
    Encoder.encodeString.contramap(timestamp => fmt.format(timestamp.value.toInstant))

  implicit val decoder: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    Either
      .catchNonFatal(Instant.parse(str))
      .map(i => Timestamp(OffsetDateTime.ofInstant(i, ZoneOffset.UTC)))
      .leftMap(_ => "Invalid timestamp format")
  }
}
