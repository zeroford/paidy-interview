package forex.http.health

import cats.effect.Sync
import forex.http.health.Protocol.HealthResponse
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class HealthHttpRoutes[F[_]: Sync] extends Http4sDsl[F] {

  private[health] val prefixPath = "/health"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok(HealthResponse("OK"))
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
