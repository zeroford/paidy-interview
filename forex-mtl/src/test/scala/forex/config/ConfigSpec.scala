package forex.config

import scala.concurrent.duration._

import com.comcast.ip4s.{ Host, Port }
import munit.FunSuite

class ConfigSpec extends FunSuite {

  test("ApplicationConfig should handle configuration correctly") {
    val config = ApplicationConfig(
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

    assertEquals(config.environment, Environment.Dev)
    assertEquals(config.http.host, Host.fromString("0.0.0.0").get)
    assertEquals(config.http.port, Port.fromInt(8080).get)
    assertEquals(config.http.timeout, 10.seconds)
    assertEquals(config.oneFrame.host, "localhost")
    assertEquals(config.oneFrame.port, 8081)
    assertEquals(config.cache.maxSize, 1000L)
    assertEquals(config.cache.ttl, 10.seconds)
    assertEquals(config.clientDefault.totalTimeout, 2.seconds)
    assertEquals(config.clientDefault.idleTimeout, 30.seconds)
    assertEquals(config.clientDefault.maxTotal, 50)
    assertEquals(config.secrets.oneFrameToken, "test-secret-token")
  }

  test("Environment should provide different environments") {
    assertEquals(Environment.Dev.toString, "Dev")
    assertEquals(Environment.Test.toString, "Test")
  }
}
