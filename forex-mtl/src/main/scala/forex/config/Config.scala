package forex.config

import cats.effect.Sync
import fs2.Stream
import com.comcast.ip4s.{ Host, Port }
import pureconfig.{ ConfigReader, ConfigSource }
import pureconfig.generic.auto._
import pureconfig.error.CannotConvert

object Config {

  implicit val hostReader: ConfigReader[Host] =
    ConfigReader[String].emap(n => Host.fromString(n).toRight(CannotConvert(n, "Host", "Invalid host")))

  implicit val portReader: ConfigReader[Port] =
    ConfigReader[Int].emap(n => Port.fromInt(n).toRight(CannotConvert(n.toString, "Port", "Invalid port")))

  def stream[F[_]: Sync](path: String): Stream[F, ApplicationConfig] =
    Stream.eval(Sync[F].delay(ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]))

}
