package forex.modules

import cats.effect.{ Async, Resource }
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

import forex.config.ClientDefault

object HttpClientBuilder {
  def build[F[_]: Async](config: ClientDefault): Resource[F, Client[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(config.totalTimeout)
      .withIdleConnectionTime(config.idleTimeout)
      .withMaxTotal(config.maxTotal)
      .build
}
