package forex

import cats.effect.{ IO, IOApp, Resource, Sync }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import forex.config.Config
import forex.modules.{ HttpClientBuilder, HttpServerBuilder }

object Main extends IOApp.Simple {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run: IO[Unit] = {
    val app: Resource[IO, Unit] = for {
      config <- Config.resource[IO]("app")
      client <- HttpClientBuilder.build[IO]
      module <- Module.make[IO](config, client)
      _ <- HttpServerBuilder.build[IO](module.httpApp, config.http)
    } yield ()

    app
      .use(_ => IO.never)
      .handleErrorWith { error =>
        IO.println(s"Application failed to start: ${error.getMessage}") *>
          IO.raiseError(error)
      }
  }
}
