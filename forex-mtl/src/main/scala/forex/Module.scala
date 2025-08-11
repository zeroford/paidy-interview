package forex

import cats.effect.Async
import forex.config.ApplicationConfig
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
    case forex.config.Environment.Dev  => 
      OneFrameClient.httpClient[F](httpClient, config.oneFrame, config.secrets.oneframeToken)
    case forex.config.Environment.Test => 
      OneFrameClient.mockClient[F]
  }

  private val cacheService: CacheService[F]  = CacheService[F](config.cache.rates.maxSize, config.cache.rates.ttl)
  private val ratesService: RatesService[F]  = RatesService[F](oneFrameClient, cacheService, config.cache.rates.ttl)
  private val ratesProgram: RatesProgram[F]  = RatesProgram[F](ratesService)
  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  private val allRoutes: HttpRoutes[F] = ratesHttpRoutes

  private val routesWithMiddleware: HttpRoutes[F] = AutoSlash(ErrorHandlerMiddleware(allRoutes))

  private val appMiddleware: HttpApp[F] = Timeout(config.http.timeout)(routesWithMiddleware.orNotFound)

  val httpApp: HttpApp[F] = appMiddleware

}
