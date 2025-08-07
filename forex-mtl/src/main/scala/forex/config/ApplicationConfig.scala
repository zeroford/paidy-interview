package forex.config

import com.comcast.ip4s.{ Host, Port }

import scala.concurrent.duration.FiniteDuration

final case class ApplicationConfig(
    environment: String,
    http: HttpConfig,
    oneFrame: OneFrameConfig
)

final case class HttpConfig(
    host: Host,
    port: Port,
    timeout: FiniteDuration
)

final case class OneFrameConfig(
    host: String,
    port: Int,
    token: String,
    timeout: FiniteDuration
)
