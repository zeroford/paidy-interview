package forex.integrations.oneframe

import cats.Applicative
import cats.effect.Concurrent
import forex.config.OneFrameConfig
import forex.integrations.oneframe.interpreter.{ HttpClient, MockClient }
import forex.integrations.OneFrameClient
import org.http4s.client.Client

object Interpreters {
  private def live[F[_]: Concurrent](client: Client[F], config: OneFrameConfig): Algebra[F] =
    new HttpClient[F](client, config)
  private def mock[F[_]: Applicative]: Algebra[F] = new MockClient[F]

  def client[F[_]: Concurrent](client: Client[F], config: OneFrameConfig, env: String): OneFrameClient[F] =
    env match {
      case "prod" | "dev" => live[F](client, config)
      case _              => mock[F]
    }
}
