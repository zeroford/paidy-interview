package forex.http
package rates

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Timestamp }
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.Encoder

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

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

  implicit val responseEncoder: Encoder[GetApiResponse]     = deriveConfiguredEncoder[GetApiResponse]
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveConfiguredEncoder[ErrorResponse]
}
