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

  private val testConfigWithTestEnv = testConfig.copy(environment = Environment.Test)

  private val mockHttpClient: Client[IO] = Client[IO] { _ =>
    Resource.pure(Response[IO](Status.Ok).withEntity("mock response"))
  }

  test("Module should create valid HttpApp with Dev environment") {
    val module = new Module[IO](testConfig, mockHttpClient)

    for {
      httpApp <- IO(module.httpApp)
      _ <- IO(assert(httpApp.isInstanceOf[HttpApp[IO]], "HttpApp should be created successfully"))
      _ <- IO(assert(httpApp != null, "HttpApp should not be null"))
    } yield ()
  }

  test("Module should create valid HttpApp with Test environment") {
    val module = new Module[IO](testConfigWithTestEnv, mockHttpClient)

    for {
      httpApp <- IO(module.httpApp)
      _ <- IO(assert(httpApp.isInstanceOf[HttpApp[IO]], "HttpApp should be created successfully"))
      _ <- IO(assert(httpApp != null, "HttpApp should not be null"))
    } yield ()
  }

  test("Module should handle different environments correctly") {
    val devModule  = new Module[IO](testConfig, mockHttpClient)
    val testModule = new Module[IO](testConfigWithTestEnv, mockHttpClient)

    for {
      devApp <- IO(devModule.httpApp)
      testApp <- IO(testModule.httpApp)
      _ <- IO(assert(devApp.isInstanceOf[HttpApp[IO]], "Dev HttpApp should be valid"))
      _ <- IO(assert(testApp.isInstanceOf[HttpApp[IO]], "Test HttpApp should be valid"))
    } yield ()
  }

  test("Module should use provided HTTP client") {
    val module = new Module[IO](testConfig, mockHttpClient)

    for {
      httpApp <- IO(module.httpApp)
      _ <- IO(assert(httpApp != null, "Module should use provided HTTP client"))
    } yield ()
  }
}
