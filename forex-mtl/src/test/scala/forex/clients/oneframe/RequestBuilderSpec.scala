package forex.clients.oneframe

import cats.effect.IO
import munit.CatsEffectSuite

import forex.config.OneFrameConfig
import forex.domain.currency.Currency
import forex.domain.rates.Rate

class RequestBuilderSpec extends CatsEffectSuite {

  val config = OneFrameConfig(
    host = "localhost",
    port = 8081
  )

  val testToken = "test-token"
  val testPairs = List(Rate.Pair(Currency.USD, Currency.JPY))

  test("RequestBuilder should build URIs correctly") {
    val builder = RequestBuilder(config.host, config.port, testToken)

    val singleRequest = builder.getRatesRequest[IO](testPairs)
    assertEquals(singleRequest.method.name, "GET")
    assertEquals(singleRequest.uri.path.toString, "/rates")
    assertEquals(singleRequest.uri.query.params.get("pair"), Some("USDJPY"))
    assertEquals(singleRequest.uri.authority.get.host.toString, "localhost")
    assertEquals(singleRequest.uri.authority.get.port, Some(8081))

    val pairs = List(
      Rate.Pair(Currency.USD, Currency.EUR),
      Rate.Pair(Currency.EUR, Currency.GBP),
      Rate.Pair(Currency.GBP, Currency.JPY)
    )
    val multiRequest = builder.getRatesRequest[IO](pairs)
    assertEquals(multiRequest.uri.query.params.get("pair"), Some("USDEUR"))
  }

  test("RequestBuilder should include authentication header") {
    val builder = RequestBuilder(config.host, config.port, testToken)
    val request = builder.getRatesRequest[IO](testPairs)

    val tokenHeader = request.headers.get(org.typelevel.ci.CIString("token"))
    assert(tokenHeader.isDefined)
    assertEquals(tokenHeader.get.head.value, testToken)
  }

  test("RequestBuilder should handle different configurations") {
    val config2 = OneFrameConfig(
      host = "different-host",
      port = 8082
    )
    val differentToken = "different-token"
    val builder        = RequestBuilder(config2.host, config2.port, differentToken)
    val request        = builder.getRatesRequest[IO](testPairs)

    assertEquals(request.uri.authority.get.host.toString, "different-host")
    assertEquals(request.uri.authority.get.port, Some(8082))
    val tokenHeader = request.headers.get(org.typelevel.ci.CIString("token"))
    assert(tokenHeader.isDefined)
    assertEquals(tokenHeader.get.head.value, differentToken)
  }

}
