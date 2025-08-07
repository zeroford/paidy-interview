package forex.integrations.oneframe

import cats.effect.IO
import forex.config.OneFrameConfig
import forex.domain.currency.Currency
import forex.domain.rates.Rate
import munit.CatsEffectSuite
import org.http4s.{ Method, Uri }

class HttpUriBuilderSpec extends CatsEffectSuite {

  val config = OneFrameConfig(
    host = "localhost",
    port = 8081,
    token = "test-token",
    timeout = scala.concurrent.duration.Duration(10, "seconds")
  )

  test("HttpUriBuilder should build correct base URI") {
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)
    val request = HttpUriBuilder.buildGetRateRequest[IO](pair, config)
    val uri     = request.uri

    assertEquals(uri.scheme, Some(Uri.Scheme.http))
    assertEquals(uri.authority.get.host, Uri.RegName("localhost"))
    assertEquals(uri.authority.get.port, Some(8081))
  }

  test("HttpUriBuilder should build correct request with query parameters") {
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)
    val request = HttpUriBuilder.buildGetRateRequest[IO](pair, config)

    assertEquals(request.method, Method.GET)
    assertEquals(request.uri.path.toString, "/rates")
    assertEquals(request.uri.query.params.get("pair"), Some("USDJPY"))
  }

  test("HttpUriBuilder should include authentication header") {
    val pair    = Rate.Pair(Currency.EUR, Currency.GBP)
    val request = HttpUriBuilder.buildGetRateRequest[IO](pair, config)

    val tokenHeader = request.headers.get(org.typelevel.ci.CIString("token"))
    assert(tokenHeader.isDefined)
    assertEquals(tokenHeader.get.head.value, "test-token")
  }

  test("HttpUriBuilder should handle different currency pairs") {
    val pairs = List(
      Rate.Pair(Currency.USD, Currency.EUR),
      Rate.Pair(Currency.GBP, Currency.JPY),
      Rate.Pair(Currency.CAD, Currency.AUD)
    )

    pairs.foreach { pair =>
      val request      = HttpUriBuilder.buildGetRateRequest[IO](pair, config)
      val expectedPair = s"${pair.from}${pair.to}"
      assertEquals(request.uri.query.params.get("pair"), Some(expectedPair))
    }
  }

  test("HttpUriBuilder should build correct URI with different configs") {
    val config2 = OneFrameConfig(
      host = "api.oneframe.com",
      port = 443,
      token = "different-token",
      timeout = scala.concurrent.duration.Duration(30, "seconds")
    )

    val pair    = Rate.Pair(Currency.USD, Currency.JPY)
    val request = HttpUriBuilder.buildGetRateRequest[IO](pair, config2)

    assertEquals(request.uri.authority.get.host, Uri.RegName("api.oneframe.com"))
    assertEquals(request.uri.authority.get.port, Some(443))
    assertEquals(request.headers.get(org.typelevel.ci.CIString("token")).get.head.value, "different-token")
  }

  test("HttpUriBuilder should handle special characters in currency codes") {
    // Test with currency codes that might have special handling
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)
    val request = HttpUriBuilder.buildGetRateRequest[IO](pair, config)

    // Verify the pair is correctly encoded in the query parameter
    val queryParam = request.uri.query.params.get("pair")
    assertEquals(queryParam, Some("USDJPY"))

    // Verify the URI is properly formatted
    assert(request.uri.toString.contains("localhost:8081/rates?pair=USDJPY"))
  }
}
