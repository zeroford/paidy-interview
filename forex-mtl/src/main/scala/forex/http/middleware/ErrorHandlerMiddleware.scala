package forex.http.middleware

import cats.MonadThrow
import cats.implicits.catsSyntaxApplicativeId
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import forex.http.util.ErrorResponse

object ErrorHandlerMiddleware {
  def apply[F[_]: MonadThrow](routes: HttpRoutes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}; import dsl._;

    HttpRoutes.of[F] { req =>
      routes.run(req).value.attempt.flatMap {
        case Right(Some(res)) => res.pure[F]
        case Right(None)      =>
          NotFound(ErrorResponse(NotFound.code, s"${req.method.name}: ${req.uri.path} not found").asJson)
        case Left(e) =>
          InternalServerError(
            ErrorResponse(InternalServerError.code, "Internal server error", Some(e.getMessage)).asJson
          )
      }
    }
  }
}
