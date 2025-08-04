package forex.http.util

import io.circe.Encoder
import io.circe.generic.semiauto._

final case class ErrorResponse(code: Int, message: String, details: List[String] = Nil)

object ErrorResponse {
  implicit val encoder: Encoder[ErrorResponse] = deriveEncoder
}