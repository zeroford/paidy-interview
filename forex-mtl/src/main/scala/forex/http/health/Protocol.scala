package forex.http.health

import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

object Protocol {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  case class HealthResponse(status: String)

  implicit val encoder: Encoder[HealthResponse] = deriveConfiguredEncoder[HealthResponse]
}
