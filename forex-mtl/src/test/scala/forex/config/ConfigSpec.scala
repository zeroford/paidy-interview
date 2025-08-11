package forex.config

import munit.FunSuite
import scala.concurrent.duration._
import com.comcast.ip4s.{ Host, Port }

class ConfigSpec extends FunSuite {

  test("ApplicationConfig should have correct default values") {
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
        rates = CacheConfig.RatesConfig(
          maxSize = 1000L,
          ttl = 10.seconds
        )
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
    assertEquals(config.cache.rates.maxSize, 1000L)
    assertEquals(config.cache.rates.ttl, 10.seconds)
    assertEquals(config.secrets.oneFrameToken, "test-secret-token")
  }

  test("Environment should have correct values") {
    assertEquals(Environment.Dev.toString, "Dev")
    assertEquals(Environment.Test.toString, "Test")
  }

  test("HttpConfig should store values correctly") {
    val httpConfig = HttpConfig(
      host = Host.fromString("localhost").get,
      port = Port.fromInt(9000).get,
      timeout = 30.seconds
    )

    assertEquals(httpConfig.host, Host.fromString("localhost").get)
    assertEquals(httpConfig.port, Port.fromInt(9000).get)
    assertEquals(httpConfig.timeout, 30.seconds)
  }

  test("OneFrameConfig should store values correctly") {
    val oneFrameConfig = OneFrameConfig(
      host = "api.example.com",
      port = 443
    )

    assertEquals(oneFrameConfig.host, "api.example.com")
    assertEquals(oneFrameConfig.port, 443)
  }

  test("CacheConfig should store values correctly") {
    val cacheConfig = CacheConfig(
      rates = CacheConfig.RatesConfig(
        maxSize = 500L,
        ttl = 5.minutes
      )
    )

    assertEquals(cacheConfig.rates.maxSize, 500L)
    assertEquals(cacheConfig.rates.ttl, 5.minutes)
  }

  test("SecretConfig should store values correctly") {
    val secretConfig = SecretConfig(
      oneFrameToken = "test-token"
    )

    assertEquals(secretConfig.oneFrameToken, "test-token")
  }
}
