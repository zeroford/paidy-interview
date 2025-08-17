package forex.http.health

import cats.Monad
import forex.http.health.Protocol.HealthResponse
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class HealthRoutes[F[_]: Monad] extends Http4sDsl[F] {

  private[health] val prefixPath = "/health"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok(HealthResponse("OK"))
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
