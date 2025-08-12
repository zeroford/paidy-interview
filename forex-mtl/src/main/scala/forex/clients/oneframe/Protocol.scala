package forex.clients.oneframe

import cats.effect.Concurrent
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
      time_stamp: String
  )

  object OneFrameRate {
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
