package forex.http.util

import cats.Applicative
import cats.syntax.applicative._
import org.http4s.{ Method, Response, Status }
import org.http4s.headers.Allow

import forex.domain.error.AppError

object ErrorMapper {

  private def getErrorResponse[F[_]: Applicative](status: Status, msg: String): F[Response[F]] =
    Response[F](status).withEntity(ErrorResponse(status.code, msg)).pure[F]

  def toErrorResponse[F[_]: Applicative](e: AppError): F[Response[F]] = e match {
    case AppError.Validation(m)             => getErrorResponse(Status.BadRequest, m)
    case AppError.NotFound(m)               => getErrorResponse(Status.NotFound, m)
    case AppError.CalculationFailed(m)      => getErrorResponse(Status.UnprocessableEntity, m)
    case AppError.UpstreamAuthFailed(_, m)  => getErrorResponse(Status.BadGateway, m)
    case AppError.RateLimited(_, m)         => getErrorResponse(Status.BadGateway, m)
    case AppError.DecodingFailed(_, m)      => getErrorResponse(Status.BadGateway, m)
    case AppError.UpstreamUnavailable(_, m) => getErrorResponse(Status.ServiceUnavailable, m)
    case AppError.UnexpectedError(m)        => getErrorResponse(Status.InternalServerError, m)
    case _ => getErrorResponse(Status.InternalServerError, "An unexpected error occurred")
  }

  def badRequest[F[_]: Applicative](messages: List[String]): F[Response[F]] = {
    val message = if (messages.isEmpty) "Bad Request: Invalid query parameters" else messages.mkString("; ")
    getErrorResponse(Status.BadRequest, message)
  }

  def methodNotAllow[F[_]: Applicative](method: Method, allow: Allow): F[Response[F]] = {
    val message = s"Method ${method} not allowed"
    Response[F](Status.MethodNotAllowed)
      .withEntity(ErrorResponse(Status.MethodNotAllowed.code, message))
      .withHeaders(allow)
      .pure[F]
  }
}
