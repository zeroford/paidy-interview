package forex.config

import com.comcast.ip4s.{ Host, Port }
import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig
)

case class HttpConfig(
    host: Host,
    port: Port,
    timeout: FiniteDuration
)
