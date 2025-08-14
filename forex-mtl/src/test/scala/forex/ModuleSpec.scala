package forex

import cats.effect.{ IO, Resource }
import forex.config.{ ApplicationConfig, Environment }
import forex.config.{ CacheConfig, HttpConfig, OneFrameConfig, SecretConfig }
import munit.CatsEffectSuite
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import com.comcast.ip4s.{ Host, Port }
import org.http4s.{ HttpApp, Response, Status }
import org.http4s.client.Client
import scala.concurrent.duration._

class ModuleSpec extends CatsEffectSuite {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val testConfig = ApplicationConfig(
    environment = Environment.Dev,
    http = HttpConfig(
      host = Host.fromString("0.0.0.0").get,
      port = Port.fromInt(8080).get,
      timeout = 10.seconds
    ),
    oneFrame = OneFrameConfig(
      host = "localhost",
      port = 8081
    ),
    cache = CacheConfig(
      rates = CacheConfig.RatesConfig(
        maxSize = 1000L,
        ttl = 10.seconds
      )
    ),
    secrets = SecretConfig(
      oneFrameToken = "test-secret-token"
    )
  )

  private val mockHttpClient: Client[IO] = Client[IO] { _ =>
    Resource.pure(Response[IO](Status.Ok).withEntity("mock response"))
  }

  test("Module should create valid HttpApp") {
    val module = new Module[IO](testConfig, mockHttpClient)

    for {
      httpApp <- IO(module.httpApp)
      _ <- IO(assert(httpApp.isInstanceOf[HttpApp[IO]], "HttpApp should be created successfully"))
    } yield ()
  }
}
