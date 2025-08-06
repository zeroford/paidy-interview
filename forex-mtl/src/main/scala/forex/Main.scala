package forex

import cats.effect._
import forex.config._
import forex.modules.{ HttpClientBuilder, HttpServerBuilder }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]
  // override def run: IO[Unit]                 = new Application[IO].stream.compile.drain
  override def run: IO[Unit] = {
    val app: Resource[IO, Unit] =
      for {
        config <- Config.resource[IO]("app")
        client <- HttpClientBuilder.build[IO]
        module = new Module[IO](config, client)
        _ <- HttpServerBuilder.build[IO](module.httpApp, config)
      } yield ()

    app.use_ // use_ = .use(_ => IO.unit), server จะ block ด้วย Stream.never ใน httpApp อยู่แล้ว
  }

}

//class Application[F[_]: Async: Network](implicit logger: Logger[F]) {
//  def stream: Stream[F, Nothing] =
//    Stream.resource(
//      for {
//        config <- Config.resource[F]("app")
//        client <- HttpClientBuilder.build[F]
//        module = new Module[F](config, client)
//        server <- HttpServerBuilder.build[F](module.httpApp, config)
//      } yield (module, server)
//    ) >> Stream.never[F]
//      .handleErrorWith { err =>
//        Stream.eval(logger.error(err)(s"Startup error")) >> Stream.raiseError[F](err)
//      }
//}
