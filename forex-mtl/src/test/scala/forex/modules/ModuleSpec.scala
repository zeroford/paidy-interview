package forex.modules

import scala.concurrent.duration._

import cats.effect.{ IO, Resource }
import com.comcast.ip4s.{ Host, Port }
import munit.CatsEffectSuite
import org.http4s.client.Client
import org.http4s.{ HttpApp, Response, Status }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import forex.config._
import forex.services.rates.concurrent.BucketLocks

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
      maxSize = 1000L,
      ttl = 10.seconds
    ),
    clientDefault = ClientDefault(
      totalTimeout = 2.seconds,
      idleTimeout = 30.seconds,
      maxTotal = 50
    ),
    secrets = SecretConfig(
      oneFrameToken = "test-secret-token"
    )
  )

  private val mockHttpClient: Client[IO] = Client[IO] { _ =>
    Resource.pure(Response[IO](Status.Ok).withEntity("mock response"))
  }

  test("Module should create valid HttpApp") {
    for {
      locks <- BucketLocks.create[IO]
      module = new Module[IO](testConfig, mockHttpClient, locks)
      httpApp <- IO(module.httpApp)
      _ <- IO(assert(httpApp.isInstanceOf[HttpApp[IO]], "HttpApp should be created successfully"))
    } yield ()
  }
}
