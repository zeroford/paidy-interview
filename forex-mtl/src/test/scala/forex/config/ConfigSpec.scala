package forex.config

import scala.concurrent.duration._
import com.comcast.ip4s.{ Host, Port }
import munit.FunSuite

class ConfigSpec extends FunSuite {

  test("ApplicationConfig builds with valid values") {
    val host = Host.fromString("0.0.0.0").getOrElse(fail("invalid host"))
    val port = Port.fromInt(8080).getOrElse(fail("invalid port"))
    val cfg  = ApplicationConfig(
      environment = Environment.Dev,
      http = HttpConfig(host = host, port = port, timeout = 10.seconds),
      oneFrame = OneFrameConfig(host = "localhost", port = 8081),
      cache = CacheConfig(maxSize = 1000L, ttl = 10.seconds),
      clientDefault = ClientDefault(totalTimeout = 2.seconds, idleTimeout = 30.seconds, maxTotal = 50),
      secrets = SecretConfig(oneFrameToken = "test-secret-token")
    )

    assertEquals(cfg.environment, Environment.Dev)
    assertEquals(cfg.http.host, host)
    assertEquals(cfg.http.port, port)
    assertEquals(cfg.http.timeout, 10.seconds)
    assertEquals(cfg.oneFrame.host, "localhost")
    assertEquals(cfg.oneFrame.port, 8081)
    assertEquals(cfg.cache.maxSize, 1000L)
    assertEquals(cfg.cache.ttl, 10.seconds)
    assertEquals(cfg.clientDefault.totalTimeout, 2.seconds)
    assertEquals(cfg.clientDefault.idleTimeout, 30.seconds)
    assertEquals(cfg.clientDefault.maxTotal, 50)
    assertEquals(cfg.secrets.oneFrameToken, "test-secret-token")
  }

  test("Environment toString for Dev and Test") {
    assertEquals(Environment.Dev.toString, "Dev")
    assertEquals(Environment.Test.toString, "Test")
  }
}
