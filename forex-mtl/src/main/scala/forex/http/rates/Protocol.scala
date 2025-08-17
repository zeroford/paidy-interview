package forex.http
package rates

import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Timestamp }

object Protocol {
  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  implicit val configuration: Configuration             = Configuration.default.withSnakeCaseMemberNames
  implicit val responseEncoder: Encoder[GetApiResponse] = deriveConfiguredEncoder[GetApiResponse]
}
