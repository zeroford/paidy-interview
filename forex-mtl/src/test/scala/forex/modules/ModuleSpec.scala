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

  private val devConfig = ApplicationConfig(
    environment = Environment.Dev,
    http = HttpConfig(
      host = Host.fromString("0.0.0.0").getOrElse(fail("invalid host")),
      port = Port.fromInt(8080).getOrElse(fail("invalid port")),
      timeout = 10.seconds
    ),
    oneFrame = OneFrameConfig(host = "localhost", port = 8081),
    cache = CacheConfig(maxSize = 1000L, ttl = 30.seconds),
    clientDefault = ClientDefault(totalTimeout = 2.seconds, idleTimeout = 30.seconds, maxTotal = 10),
    secrets = SecretConfig(oneFrameToken = "token")
  )

  private val testConfig = devConfig.copy(environment = Environment.Test)

  private val okClient: Client[IO] = Client[IO](_ => Resource.pure(Response[IO](Status.Ok)))

  private val locksResource: Resource[IO, forex.services.rates.concurrent.BucketLocks[IO]] =
    Resource.eval(BucketLocks.create[IO])

  test("Module should create valid HttpApp with Dev environment") {
    locksResource.use { locks =>
      for {
        module <- IO(new Module[IO](devConfig, okClient, locks))
        app = module.httpApp
      } yield assert(app.isInstanceOf[HttpApp[IO]])
    }
  }

  test("Module should create valid HttpApp with Test environment") {
    locksResource.use { locks =>
      for {
        module <- IO(new Module[IO](testConfig, okClient, locks))
        app = module.httpApp
      } yield assert(app.isInstanceOf[HttpApp[IO]])
    }
  }

  test("HttpApp should respond NotFound for unknown route") {
    locksResource.use { locks =>
      for {
        module <- IO(new Module[IO](devConfig, okClient, locks))
        app = module.httpApp
        resp <- app.run(org.http4s.Request[IO](org.http4s.Method.GET, org.http4s.Uri.unsafeFromString("/not-found")))
      } yield assertEquals(resp.status, Status.NotFound)
    }
  }
}
