package forex.config

import scala.concurrent.duration.FiniteDuration

import com.comcast.ip4s.{ Host, Port }

final case class ApplicationConfig(
    environment: Environment,
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    cache: CacheConfig,
    clientDefault: ClientDefault,
    secrets: SecretConfig
)

final case class HttpConfig(
    host: Host,
    port: Port,
    timeout: FiniteDuration
)

final case class OneFrameConfig(
    host: String,
    port: Int
)

final case class CacheConfig(
    maxSize: Long,
    ttl: FiniteDuration
)

final case class ClientDefault(
    totalTimeout: FiniteDuration,
    idleTimeout: FiniteDuration,
    maxTotal: Int
)

final case class SecretConfig(
    oneFrameToken: String = ""
)
