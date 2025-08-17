package forex.modules

import cats.effect.{ Async, Resource }
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

object HttpClientBuilder {
  def build[F[_]: Async]: Resource[F, Client[F]] =
    EmberClientBuilder.default[F].build
}
