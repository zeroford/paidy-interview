package forex

import cats.effect.Async
import cats.implicits.toSemigroupKOps
import forex.config.{ ApplicationConfig, Environment }
import forex.http.health.HealthHttpRoutes
import forex.http.middleware.ErrorHandlerMiddleware
import forex.http.rates.RatesHttpRoutes
import forex.integrations.OneFrameClient
import forex.programs.RatesProgram
import forex.services.{ CacheService, RatesService }
import org.http4s._
import org.http4s.client.Client
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Async](config: ApplicationConfig, httpClient: Client[F]) {

  private val oneFrameClient: OneFrameClient[F] = config.environment match {
    case Environment.Dev =>
      OneFrameClient.httpClient[F](httpClient, config.oneFrame, config.secrets.oneFrameToken)
    case Environment.Test =>
      OneFrameClient.mockClient[F]
  }

  private val cacheService: CacheService[F]  = CacheService[F](config.cache.rates.maxSize, config.cache.rates.ttl)
  private val ratesService: RatesService[F]  = RatesService[F](oneFrameClient, cacheService, config.cache.rates.ttl)
  private val ratesProgram: RatesProgram[F]  = RatesProgram[F](ratesService)
  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes
  private val healthRoutes: HttpRoutes[F]    = new HealthHttpRoutes[F].routes

  private val allRoutes: HttpRoutes[F] = ratesHttpRoutes <+> healthRoutes

  private val routesWithMiddleware: HttpRoutes[F] = AutoSlash(ErrorHandlerMiddleware(allRoutes))

  private val appMiddleware: HttpApp[F] = Timeout(config.http.timeout)(routesWithMiddleware.orNotFound)

  val httpApp: HttpApp[F] = appMiddleware

}
