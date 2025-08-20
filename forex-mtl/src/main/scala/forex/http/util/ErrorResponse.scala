package forex.http.util

import io.circe.{ Encoder, Json }
import io.circe.syntax._

final case class ErrorResponse(code: Int, message: String, details: Option[String] = None)

object ErrorResponse {
  implicit val encoder: Encoder[ErrorResponse] = new Encoder[ErrorResponse] {
    override def apply(response: ErrorResponse): Json = {
      val base = Json.obj(
        "code" -> response.code.asJson,
        "message" -> response.message.asJson
      )

      response.details match {
        case Some(detail) if detail.nonEmpty =>
          base.deepMerge(Json.obj("details" -> detail.asJson))
        case _ => base
      }
    }
  }
}
