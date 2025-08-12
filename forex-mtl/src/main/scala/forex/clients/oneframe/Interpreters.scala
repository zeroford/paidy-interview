package forex.clients.oneframe

import cats.Applicative
import cats.effect.Concurrent
import forex.config.OneFrameConfig
import forex.clients.oneframe.interpreter.{ HttpClient, MockClient }
import forex.clients.OneFrameClient
import org.http4s.client.Client

object Interpreters {
  def httpClient[F[_]: Concurrent](client: Client[F], config: OneFrameConfig, token: String): OneFrameClient[F] =
    new HttpClient[F](client, config, token)

  def mockClient[F[_]: Applicative]: OneFrameClient[F] = new MockClient[F]
}
