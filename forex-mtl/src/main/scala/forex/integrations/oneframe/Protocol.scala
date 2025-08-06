package forex.integrations.oneframe

import cats.effect.Concurrent
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

object Protocol {

  final case class GetRateResponse(rates: List[ExchangeRate])

  final case class ExchangeRate(
      from: String,
      to: String,
      bid: BigDecimal,
      ask: BigDecimal,
      price: BigDecimal,
      time_stamp: String
  )
  object GetRateResponse {
    implicit val decoder: Decoder[GetRateResponse]                                  = deriveDecoder[GetRateResponse]
    implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, GetRateResponse] = jsonOf[F, GetRateResponse]
  }

  final case class OneFrameApiError(error: String)
  object OneFrameApiError {
    implicit val decoder: Decoder[OneFrameApiError] = Decoder.forProduct1("error")(OneFrameApiError.apply)
    implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, OneFrameApiError] = jsonOf[F, OneFrameApiError]
  }
}
