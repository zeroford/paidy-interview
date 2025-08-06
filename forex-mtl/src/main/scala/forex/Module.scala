package forex

import cats.effect.Async
import forex.config.ApplicationConfig
import forex.http.middleware.ErrorHandlerMiddleware
import forex.http.rates.RatesHttpRoutes
import forex.integrations.{ OneFrameClient, OneFrameDummyClient, OneFrameHttpClient }
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.client.Client
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Async](config: ApplicationConfig, client: Client[F]) {

  private val oneFrameHttpClient: OneFrameClient[F]  = OneFrameHttpClient[F](client, config.oneFrame)
  private val oneFrameDummyClient: OneFrameClient[F] = OneFrameDummyClient[F]

  private val ratesService: RatesService[F]  = RatesServices[F]
  private val ratesProgram: RatesProgram[F]  = RatesProgram[F](ratesService)
  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  private val allRoutes: HttpRoutes[F] = ratesHttpRoutes

  private val routesWithMiddleware: HttpRoutes[F] = AutoSlash(ErrorHandlerMiddleware(allRoutes))

  private val appMiddleware: HttpApp[F] = Timeout(config.http.timeout)(routesWithMiddleware.orNotFound)

  val httpApp: HttpApp[F] = appMiddleware

}
