package forex.clients.oneframe

import cats.Applicative
import cats.effect.Concurrent
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

import forex.clients.OneFrameClient
import forex.clients.oneframe.interpreter.{ HttpClient, MockClient }
import forex.config.OneFrameConfig

object Interpreters {
  def httpClient[F[_]: Concurrent: Logger](
      client: Client[F],
      config: OneFrameConfig,
      token: String
  ): OneFrameClient[F] =
    new HttpClient[F](client, config, token)

  def mockClient[F[_]: Applicative: Logger]: OneFrameClient[F] = new MockClient[F]
}
