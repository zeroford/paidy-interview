package forex.modules

import cats.effect.{ Async, Resource }
import forex.config.ApplicationConfig
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger

object HttpServerBuilder {
  private def showEmberBanner[F[_]](s: Server)(implicit logger: Logger[F]): F[Unit] =
    logger.info(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}")

  def build[F[_]: Async](httpApp: HttpApp[F], config: ApplicationConfig)(implicit
      logger: Logger[F]
  ): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(config.http.host)
      .withPort(config.http.port)
      .withHttpApp(httpApp)
      .build
      .evalTap(showEmberBanner[F])
}
