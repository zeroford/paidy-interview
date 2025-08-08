package forex.config

import cats.effect.{ Resource, Sync }
import com.comcast.ip4s.{ Host, Port }
import pureconfig.{ ConfigReader, ConfigSource }
import pureconfig.generic.auto._
import pureconfig.error.CannotConvert

object Config {

  implicit val environmentReader: ConfigReader[Environment] =
    ConfigReader[String].emap {
      case "dev"  => Right(Environment.Dev)
      case "test" => Right(Environment.Test)
      case other  => Left(CannotConvert(other, "Environment", s"Invalid environment: $other"))
    }

  implicit val hostReader: ConfigReader[Host] =
    ConfigReader[String].emap(n => Host.fromString(n).toRight(CannotConvert(n, "Host", s"Invalid host: $n")))

  implicit val portReader: ConfigReader[Port] =
    ConfigReader[Int].emap(n => Port.fromInt(n).toRight(CannotConvert(n.toString, "Port", s"Invalid port: $n")))

  def resource[F[_]: Sync](path: String): Resource[F, ApplicationConfig] =
    Resource.eval(Sync[F].delay {
      ConfigSource.default
        .withFallback(ConfigSource.systemProperties)
        .at(path)
        .loadOrThrow[ApplicationConfig]
    })
}
