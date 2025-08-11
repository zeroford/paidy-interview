package forex.http
package rates

import forex.domain.rates.Rate.Pair
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  final case class ErrorResponse(
      error: String,
      message: String,
      details: Option[Map[String, String]] = None
  )

  // JSON Encoders/Decoders
  implicit val pairEncoder: Encoder[Pair]                   = deriveConfiguredEncoder[Pair]
  implicit val rateEncoder: Encoder[Rate]                   = deriveConfiguredEncoder[Rate]
  implicit val responseEncoder: Encoder[GetApiResponse]     = deriveConfiguredEncoder[GetApiResponse]
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveConfiguredEncoder[ErrorResponse]
}
