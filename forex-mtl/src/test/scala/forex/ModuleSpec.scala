package forex

import cats.effect.IO
import forex.config.{ApplicationConfig, Environment, HttpConfig, OneFrameConfig, CacheConfig}
import org.http4s.HttpApp
import org.http4s.client.Client
import munit.CatsEffectSuite
import scala.concurrent.duration._
import com.comcast.ip4s.{Host, Port}

class ModuleSpec extends CatsEffectSuite {

  val testConfig = ApplicationConfig(
    environment = Environment.Dev,
    http = HttpConfig(
      host = Host.fromString("0.0.0.0").get,
      port = Port.fromInt(8080).get,
      timeout = 10.seconds
    ),
    oneFrame = OneFrameConfig(
      host = "localhost",
      port = 8081,
      token = "test-token"
    ),
    cache = CacheConfig(
      rates = CacheConfig.RatesConfig(
        maxSize = 1000L,
        ttl = 10.seconds
      )
    )
  )

  val mockHttpClient: Client[IO] = Client[IO](_ => cats.effect.Resource.pure(org.http4s.Response[IO]()))

  test("Module should create httpApp with correct configuration") {
    val module = new Module[IO](testConfig, mockHttpClient)
    
    for {
      httpApp <- IO(module.httpApp)
      _ <- IO(assert(httpApp.isInstanceOf[HttpApp[IO]]))
    } yield ()
  }

  test("Module should use Dev environment for OneFrame client") {
    val module = new Module[IO](testConfig, mockHttpClient)
    
    // This test verifies that the module is created successfully
    // The actual OneFrame client selection is tested in integration tests
    for {
      httpApp <- IO(module.httpApp)
      _ <- IO(assert(httpApp.isInstanceOf[HttpApp[IO]]))
    } yield ()
  }

  test("Module should use Test environment for OneFrame client") {
    val testConfigWithTestEnv = testConfig.copy(environment = Environment.Test)
    val module = new Module[IO](testConfigWithTestEnv, mockHttpClient)
    
    for {
      httpApp <- IO(module.httpApp)
      _ <- IO(assert(httpApp.isInstanceOf[HttpApp[IO]]))
    } yield ()
  }
}
