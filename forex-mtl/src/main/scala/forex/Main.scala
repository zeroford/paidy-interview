package forex

import cats.effect._
import fs2.io.net.Network
import forex.config._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]
  override def run: IO[Unit]                 = new Application[IO].stream.compile.drain

}

class Application[F[_]: Async: Network](implicit logger: Logger[F]) {

  private def showEmberBanner(s: Server): F[Unit] =
    logger.info(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}")

  def stream: Stream[F, Nothing] =
    Config
      .stream[F]("app")
      .flatMap { config =>
        val module = new Module[F](config)
        Stream
          .resource(
            EmberServerBuilder
              .default[F]
              .withHost(config.http.host)
              .withPort(config.http.port)
              .withHttpApp(module.httpApp)
              .build
              .evalTap(showEmberBanner)
          )
          .flatMap(_ => Stream.never[F])
      }
      .handleErrorWith { err =>
        Stream.eval(logger.error(err)(s"Startup error")) >> Stream.raiseError[F](err)
      }
}
