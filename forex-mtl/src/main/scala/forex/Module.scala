package forex

import cats.effect.{ Concurrent, Timer }
import forex.config.ApplicationConfig
import forex.http.middleware.ErrorHandlerMiddleware
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig) {

  private val ratesService: RatesService[F]  = RatesServices.dummy[F]
  private val ratesProgram: RatesProgram[F]  = RatesProgram[F](ratesService)
  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  private val allRoutes: HttpRoutes[F] = ratesHttpRoutes

  private val routesMiddleware: HttpRoutes[F] = AutoSlash(ErrorHandlerMiddleware(allRoutes))

  private val appMiddleware: HttpApp[F] = Timeout(config.http.timeout)(routesMiddleware.orNotFound)

  val httpApp: HttpApp[F] = appMiddleware
}
