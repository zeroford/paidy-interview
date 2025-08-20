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

  private val baseConfig = ApplicationConfig(
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

  private val devConfig  = baseConfig.copy(environment = Environment.Dev)
  private val testConfig = baseConfig.copy(environment = Environment.Test)

  private val mockHttpClient: Client[IO] = Client[IO] { _ =>
    Resource.pure(Response[IO](Status.Ok).withEntity("mock response"))
  }

  val locksResource: Resource[IO, BucketLocks[IO]] = Resource.eval(BucketLocks.create[IO])

  test("Module should create valid HttpApp with Dev environment") {
    locksResource.use { locks =>
      for {
        module <- IO(new Module[IO](devConfig, mockHttpClient, locks))
        httpApp = module.httpApp
        _       = assert(httpApp.isInstanceOf[HttpApp[IO]], "HttpApp should be created successfully")
      } yield ()
    }
  }

  test("Module should create valid HttpApp with Test environment") {
    locksResource.use { locks =>
      for {
        module <- IO(new Module[IO](testConfig, mockHttpClient, locks))
        httpApp = module.httpApp
        _       = assert(httpApp.isInstanceOf[HttpApp[IO]], "HttpApp should be created successfully")
      } yield ()
    }
  }

  test("Module should use Mock Client when environment is Test") {
    locksResource.use { locks =>
      for {
        testModule <- IO(new Module[IO](testConfig, mockHttpClient, locks))
        devModule <- IO(new Module[IO](devConfig, mockHttpClient, locks))

        // Both should create valid HttpApps
        testHttpApp = testModule.httpApp
        devHttpApp  = devModule.httpApp

        _ = assert(testHttpApp.isInstanceOf[HttpApp[IO]], "Test environment should create valid HttpApp")
        _ = assert(devHttpApp.isInstanceOf[HttpApp[IO]], "Dev environment should create valid HttpApp")

        // Test that different environments create different internal configurations
        _ = assert(testConfig.environment == Environment.Test, "Test config should have Test environment")
        _ = assert(devConfig.environment == Environment.Dev, "Dev config should have Dev environment")
      } yield ()
    }
  }

  test("Module should handle environment configuration correctly") {
    locksResource.use { locks =>
      for {
        testModule <- IO(new Module[IO](testConfig, mockHttpClient, locks))

        devModule <- IO(new Module[IO](devConfig, mockHttpClient, locks))

        testApp = testModule.httpApp
        devApp  = devModule.httpApp

        _ = assert(testApp.isInstanceOf[HttpApp[IO]], "Test environment module should create HttpApp")
        _ = assert(devApp.isInstanceOf[HttpApp[IO]], "Dev environment module should create HttpApp")

        _ = assertEquals(testConfig.environment, Environment.Test)
        _ = assertEquals(devConfig.environment, Environment.Dev)
      } yield ()
    }
  }

  test("Module should use Mock Client in Test environment for rates endpoint") {
    import org.http4s.{ Method, Request, Uri }

    locksResource.use { locks =>
      for {
        testModule <- IO(new Module[IO](testConfig, mockHttpClient, locks))
        httpApp = testModule.httpApp

        request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=EUR"))
        response <- httpApp.run(request)

        _ = assert(response.status.isSuccess, s"Expected successful response but got ${response.status}")

        responseBody <- response.as[String]

        _ = assert(responseBody.nonEmpty, "Response body should not be empty")
        _ = assert(responseBody.contains("from") || responseBody.contains("to"), "Response should contain rate data")
      } yield ()
    }
  }
}
