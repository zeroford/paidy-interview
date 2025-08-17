package forex.config

import scala.concurrent.duration.FiniteDuration

import com.comcast.ip4s.{ Host, Port }

final case class ApplicationConfig(
    environment: Environment,
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    cache: CacheConfig,
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
    rates: CacheConfig.RatesConfig
)

object CacheConfig {
  final case class RatesConfig(
      maxSize: Long,
      ttl: FiniteDuration
  )
}

final case class SecretConfig(
    oneFrameToken: String = ""
)
