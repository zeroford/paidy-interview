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
    port = 8081
  )

  val testToken = "test-token"

  test("HttpUriBuilder should build correct base URI") {
    val pairs   = List(Rate.Pair(Currency.USD, Currency.JPY))
    val request = UriBuilder.buildGetRatesRequest[IO](pairs, config, testToken)
    val uri     = request.uri

    assertEquals(uri.scheme, Some(Uri.Scheme.http))
    assertEquals(uri.authority.get.host, Uri.RegName("localhost"))
    assertEquals(uri.authority.get.port, Some(8081))
  }

  test("HttpUriBuilder should build correct request with query parameters") {
    val pairs   = List(Rate.Pair(Currency.USD, Currency.JPY))
    val request = UriBuilder.buildGetRatesRequest[IO](pairs, config, testToken)

    assertEquals(request.method, Method.GET)
    assertEquals(request.uri.path.toString, "/rates")
    assertEquals(request.uri.query.params.get("pair"), Some("USDJPY"))
  }

  test("HttpUriBuilder should include authentication header") {
    val pairs   = List(Rate.Pair(Currency.EUR, Currency.GBP))
    val request = UriBuilder.buildGetRatesRequest[IO](pairs, config, testToken)

    val tokenHeader = request.headers.get(org.typelevel.ci.CIString("token"))
    assert(tokenHeader.isDefined)
    assertEquals(tokenHeader.get.head.value, "test-token")
  }

  test("HttpUriBuilder should handle multiple currency pairs") {
    val testPairs = List(
      Rate.Pair(Currency.USD, Currency.EUR),
      Rate.Pair(Currency.GBP, Currency.JPY),
      Rate.Pair(Currency.CAD, Currency.AUD)
    )

    val request   = UriBuilder.buildGetRatesRequest[IO](testPairs, config, testToken)
    val uriString = request.uri.toString

    assert(uriString.contains("pair=CADAUD"))
    assertEquals(request.method, Method.GET)
    assertEquals(request.uri.path.toString, "/rates")
  }

  test("HttpUriBuilder should build correct URI with different configs") {
    val config2 = OneFrameConfig(
      host = "api.oneframe.com",
      port = 443
    )

    val differentToken = "different-token"
    val pairs          = List(Rate.Pair(Currency.USD, Currency.JPY))
    val request        = UriBuilder.buildGetRatesRequest[IO](pairs, config2, differentToken)

    assertEquals(request.uri.authority.get.host, Uri.RegName("api.oneframe.com"))
    assertEquals(request.uri.authority.get.port, Some(443))
    assertEquals(request.headers.get(org.typelevel.ci.CIString("token")).get.head.value, "different-token")
  }

  test("HttpUriBuilder should handle special characters in currency codes") {
    val pairs   = List(Rate.Pair(Currency.USD, Currency.JPY))
    val request = UriBuilder.buildGetRatesRequest[IO](pairs, config, testToken)

    val queryParam = request.uri.query.params.get("pair")
    assertEquals(queryParam, Some("USDJPY"))

    assert(request.uri.toString.contains("localhost:8081/rates?pair=USDJPY"))
  }
}
