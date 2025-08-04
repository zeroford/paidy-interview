package forex.http.util

import cats.effect.Sync
import forex.programs.rates.errors.Error
import org.http4s.{Response, Status}
import org.http4s.dsl.Http4sDsl

object HttpErrorMapper {
  def map[F[_]: Sync](error: Error): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}; import dsl._

    error match {
      case Error.RateLookupFailed(_) =>
        BadGateway(response(BadGateway, "External rate provider failed"))
    }

  }

  def badRequest[F[_]: Sync](messages: List[String]): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}; import dsl._
    BadRequest(response(Status.BadRequest, "Bad Request: Invalid query parameters", messages))
  }

  private def response(status: Status, message: String, details: List[String] = Nil): ErrorResponse =
    ErrorResponse(status.code, message, details)
}