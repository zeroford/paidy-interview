package forex.http.middleware

import cats.effect.Sync
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps }
import forex.util.ErrorResponse
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.syntax._

object ErrorHandlerMiddleware {
  def apply[F[_]: Sync](routes: HttpRoutes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] { req =>
      routes.run(req).value.attempt.flatMap {
        case Right(Some(resp)) => Sync[F].pure(resp)
        case Right(None)       => NotFound(ErrorResponse(NotFound.code, s"${req.method.name}: ${req.uri.path} not found").asJson)
        case Left(e)           => InternalServerError(ErrorResponse(InternalServerError.code, e.getMessage).asJson)
      }
    }
  }
}
