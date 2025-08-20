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

/**
 * Test-specific module factory that bypasses HttpClientBuilder and HttpServerBuilder
 * for unit testing purposes.
 */
object TestModule {

  /**
   * Creates a module for testing with a mock HTTP client
   */
  def makeWithMockClient[F[_]: Async: Logger](
      config: ApplicationConfig,
      mockClient: Client[F]
  ): Resource[F, Module[F]] =
    Resource.eval(BucketLocks.create[F].map(locks => new Module[F](config, mockClient, locks)))

  /**
   * Creates a module for testing with a custom OneFrame client
   */
  def makeWithCustomOneFrameClient[F[_]: Async: Logger](
      config: ApplicationConfig,
      oneFrameClient: OneFrameClient[F]
  ): Resource[F, TestModule[F]] =
    Resource.eval(BucketLocks.create[F].map(locks => new TestModule[F](config, oneFrameClient, locks)))

  /**
   * Creates a module for testing with all mocked dependencies
   */
  def makeWithMockDependencies[F[_]: Async: Logger](
      config: ApplicationConfig,
      oneFrameClient: OneFrameClient[F],
      cacheService: CacheService[F]
  ): Resource[F, TestModule[F]] =
    Resource.eval(BucketLocks.create[F].map(locks => new TestModule[F](config, oneFrameClient, locks, Some(cacheService))))
}

/**
 * Test-specific module that allows injection of mocked dependencies
 */
final class TestModule[F[_]: Async: Logger](
    config: ApplicationConfig,
    oneFrameClient: OneFrameClient[F],
    locks: BucketLocks[F],
    customCacheService: Option[CacheService[F]] = None
) {

  private val cacheService: CacheService[F] = customCacheService.getOrElse(
    CacheService[F](config.cache.maxSize, config.cache.ttl)
  )
  
  private val ratesService: RatesService[F] =
    RatesService[F](oneFrameClient, cacheService, locks, config.cache.ttl)
  
  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)
  private val ratesHttpRoutes: HttpRoutes[F] = new RatesRoutes[F](ratesProgram).routes
  private val healthRoutes: HttpRoutes[F] = new HealthRoutes[F].routes

  private val allRoutes: HttpRoutes[F] = ratesHttpRoutes <+> healthRoutes

  private val routesWithMiddleware: HttpRoutes[F] = AutoSlash(ErrorHandlerMiddleware(allRoutes))

  private val appMiddleware: HttpApp[F] = Timeout(config.http.timeout)(routesWithMiddleware.orNotFound)

  val httpApp: HttpApp[F] = appMiddleware

  // Expose services for testing
  val ratesServiceForTesting: RatesService[F] = ratesService
  val ratesProgramForTesting: RatesProgram[F] = ratesProgram
  val cacheServiceForTesting: CacheService[F] = cacheService
}
