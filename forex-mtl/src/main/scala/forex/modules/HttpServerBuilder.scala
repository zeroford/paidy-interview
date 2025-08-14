package forex.modules

import cats.effect.{ Async, Resource }
import forex.config.HttpConfig
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger

object HttpServerBuilder {

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}")

  def build[F[_]: Async: Logger](httpApp: HttpApp[F], config: HttpConfig): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(httpApp)
      .build
      .evalTap(showEmberBanner[F])
}
