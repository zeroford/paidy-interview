package forex

import cats.effect._
import fs2.io.net.Network
import forex.config._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._

object Main extends IOApp.Simple {

  override def run: IO[Unit] = new Application[IO].stream.compile.drain
}

class Application[F[_]: Async: Network] {

  def stream: Stream[F, Unit] =
    Config.stream[F]("app").flatMap { config =>
      val module = new Module[F](config)
      Stream
        .resource(
          EmberServerBuilder
            .default[F]
            .withHost(Host.fromString(config.http.host).getOrElse(host"0.0.0.0"))
            .withPort(Port.fromInt(config.http.port).getOrElse(port"8080"))
            .withHttpApp(module.httpApp)
            .build
        )
        .drain
    }
}
