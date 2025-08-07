package forex.http.util

import cats.effect.Sync
import forex.programs.rates.errors.RateProgramError
import io.circe.syntax.EncoderOps
import org.http4s.{ Method, Response, Status }
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Allow
import org.http4s.circe._

object HttpErrorMapper {
  def map[F[_]: Sync](error: RateProgramError): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}; import dsl._
    error match {
      case RateProgramError.RateLookupFailed(_) =>
        BadGateway(ErrorResponse(Status.BadGateway.code, "External rate provider failed"))
      case RateProgramError.ValidationFailed(errors) =>
        BadRequest(ErrorResponse(Status.BadRequest.code, "Validation failed", errors).asJson)
    }
  }

  def badRequest[F[_]: Sync](messages: List[String]): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}; import dsl._
    BadRequest(ErrorResponse(Status.BadRequest.code, "Bad Request: Invalid query parameters", messages).asJson)
  }

  def methodNotAllow[F[_]: Sync](method: Method): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}; import dsl._
    MethodNotAllowed(Allow(), ErrorResponse(Status.MethodNotAllowed.code, s"Method ${method} not allowed").asJson)
  }
}
