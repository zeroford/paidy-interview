package forex.modules

import cats.effect.{ Async, Resource }
import cats.syntax.functor._
import cats.syntax.semigroupk._

import forex.clients.OneFrameClient
import forex.config.{ ApplicationConfig, Environment }
import forex.http.health.HealthRoutes
import forex.http.rates.RatesRoutes
import forex.modules.middleware.ErrorHandlerMiddleware
import forex.programs.RatesProgram
import forex.services.rates.concurrent.BucketLocks
import forex.services.{ CacheService, RatesService }
import org.http4s._
import org.http4s.client.Client
import org.http4s.server.middleware.{ AutoSlash, Timeout }
import org.typelevel.log4cats.Logger

final class Module[F[_]: Async: Logger](config: ApplicationConfig, httpClient: Client[F], locks: BucketLocks[F]) {

  private val oneFrameClient: OneFrameClient[F] = config.environment match {
    case Environment.Dev =>
      OneFrameClient.httpClient[F](httpClient, config.oneFrame, config.secrets.oneFrameToken)
    case Environment.Test =>
      OneFrameClient.mockClient[F]
  }

  private val cacheService: CacheService[F] = CacheService[F](config.cache.maxSize, config.cache.ttl)
  private val ratesService: RatesService[F] =
    RatesService[F](oneFrameClient, cacheService, locks, config.cache.ttl)
  private val ratesProgram: RatesProgram[F]  = RatesProgram[F](ratesService)
  private val ratesHttpRoutes: HttpRoutes[F] = new RatesRoutes[F](ratesProgram).routes
  private val healthRoutes: HttpRoutes[F]    = new HealthRoutes[F].routes

  private val allRoutes: HttpRoutes[F] = ratesHttpRoutes <+> healthRoutes

  private val routesWithMiddleware: HttpRoutes[F] = AutoSlash(ErrorHandlerMiddleware(allRoutes))

  private val appMiddleware: HttpApp[F] = Timeout(config.http.timeout)(routesWithMiddleware.orNotFound)

  val httpApp: HttpApp[F] = appMiddleware

}

object Module {
  def make[F[_]: Async: Logger](config: ApplicationConfig, httpClient: Client[F]): Resource[F, Module[F]] =
    Resource.eval(BucketLocks.create[F].map(locks => new Module[F](config, httpClient, locks)))
}
