package forex.config

import com.comcast.ip4s.{ Host, Port }

import scala.concurrent.duration.FiniteDuration

final case class ApplicationConfig(
    environment: Environment,
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    cache: CacheConfig
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
