package forex.clients.oneframe

import java.time.{ Instant, OffsetDateTime }
import java.time.format.DateTimeFormatter

import cats.effect.Concurrent
import cats.syntax.either._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

object Protocol {

  type OneFrameRatesResponse = List[OneFrameRate]

  final case class OneFrameRate(
      from: String,
      to: String,
      bid: BigDecimal,
      ask: BigDecimal,
      price: BigDecimal,
      time_stamp: Instant
  )

  object OneFrameRate {
    implicit val instantDecoder: Decoder[Instant] =
      Decoder.decodeString.emap { s0 =>
        val s = s0.trim
        Either
          .catchNonFatal(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(s)))
          .orElse(Either.catchNonFatal(OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant))
          .leftMap(_ => s"Invalid provider time_stamp: $s")
      }
    implicit val decoder: Decoder[OneFrameRate]                                  = deriveDecoder[OneFrameRate]
    implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, OneFrameRate] = jsonOf[F, OneFrameRate]
  }

  object OneFrameRatesResponse {
    implicit val decoder: Decoder[OneFrameRatesResponse] = deriveDecoder[OneFrameRatesResponse]
    implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, OneFrameRatesResponse] =
      jsonOf[F, OneFrameRatesResponse]
  }

  final case class OneFrameApiError(error: String)
  object OneFrameApiError {
    implicit val decoder: Decoder[OneFrameApiError] = Decoder.forProduct1("error")(OneFrameApiError.apply)
    implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, OneFrameApiError] = jsonOf[F, OneFrameApiError]
  }
}
