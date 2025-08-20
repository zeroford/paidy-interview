package forex.clients.oneframe

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import forex.clients.oneframe.interpreter.HttpClient
import forex.config.OneFrameConfig
import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.Rate

class HttpClientSpec extends CatsEffectSuite {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val config = OneFrameConfig(
    host = "localhost",
    port = 8081
  )

  val mockClient: Client[IO] = Client[IO](req =>
    cats.effect.Resource.pure {
      val uri = req.uri.toString()
      if (uri.contains("pair=")) {
        val allPairs = uri.split("pair=").drop(1).map(_.split("&")(0)).toList
        if (allPairs.isEmpty) {
          org.http4s.Response[IO](org.http4s.Status.Ok).withEntity("[]")
        } else {
          val pairList = allPairs
          val rates    = pairList
            .map { pair =>
              val from = pair.take(3)
              val to   = pair.drop(3)
              s"""{"from":"$from","to":"$to","bid":0.0085,"ask":0.0086,"price":0.00855,"time_stamp":"2023-01-01T00:00:00.000000Z"}"""
            }
            .mkString("[", ",", "]")
          org.http4s.Response[IO](org.http4s.Status.Ok).withEntity(rates)
        }
      } else {
        org.http4s.Response[IO](org.http4s.Status.Ok).withEntity("[]")
      }
    }
  )

  test("HttpClient should handle successful response") {
    val pairs      = List(Rate.Pair(Currency.USD, Currency.JPY))
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
    } yield result match {
      case Right(rates) =>
        assert(rates.nonEmpty)
        val firstRate = rates.head
        assertEquals(firstRate.currency, Currency.JPY)
        assert(firstRate.price.value > 0)
      case Left(e) =>
        fail(s"Unexpected error: $e")
    }
  }

  test("HttpClient should handle multiple currency pairs") {
    val pairs = List(
      Rate.Pair(Currency.USD, Currency.EUR),
      Rate.Pair(Currency.EUR, Currency.GBP),
      Rate.Pair(Currency.GBP, Currency.JPY)
    )
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
    } yield result match {
      case Right(rates) =>
        assertEquals(rates.size, 3)
        assert(rates.forall(_.price.value > 0))
      case Left(e) =>
        fail(s"Unexpected error: $e")
    }
  }

  test("HttpClient should handle EUR/GBP pair") {
    val pairs      = List(Rate.Pair(Currency.EUR, Currency.GBP))
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
    } yield result match {
      case Right(rates) =>
        assert(rates.nonEmpty)
        val firstRate = rates.head
        assertEquals(firstRate.currency, Currency.GBP)
        assert(firstRate.price.value > 0)
      case Left(e) =>
        fail(s"Unexpected error: $e")
    }
  }

  test("HttpClient should handle USD/EUR pair") {
    val pairs      = List(Rate.Pair(Currency.USD, Currency.EUR))
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
    } yield result match {
      case Right(rates) =>
        assert(rates.nonEmpty)
        val firstRate = rates.head
        assertEquals(firstRate.currency, Currency.EUR)
        assert(firstRate.price.value > 0)
      case Left(e) =>
        fail(s"Unexpected error: $e")
    }
  }

  test("HttpClient should handle edge cases") {
    val edgePairs = List(
      Rate.Pair(Currency.USD, Currency.USD),
      Rate.Pair(Currency.EUR, Currency.EUR),
      Rate.Pair(Currency.GBP, Currency.GBP)
    )
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(edgePairs)
    } yield result match {
      case Right(rates) =>
        assert(rates.nonEmpty)
        assert(rates.forall(_.price.value > 0))
      case Left(e) =>
        fail(s"Unexpected error: $e")
    }
  }

  test("HttpClient should handle different configurations") {
    val pairs           = List(Rate.Pair(Currency.USD, Currency.JPY))
    val differentConfig = config.copy(host = "different-host", port = 8082)
    val differentToken  = "different-token"
    val httpClient      = HttpClient[IO](mockClient, differentConfig, differentToken)

    for {
      result <- httpClient.getRates(pairs)
    } yield result match {
      case Right(rates) =>
        assert(rates.nonEmpty)
        val firstRate = rates.head
        assertEquals(firstRate.currency, Currency.JPY)
        assert(firstRate.price.value > 0)
      case Left(e) =>
        fail(s"Unexpected error: $e")
    }
  }

  test("HttpClient should handle empty pairs list") {
    val pairs      = List.empty[Rate.Pair]
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
    } yield result match {
      case Left(_: AppError.NotFound) => ()
      case other                      => fail(s"Expected NotFound for empty pairs, got: $other")
    }
  }

  test("HttpClient should handle JSON decode errors") {
    val errorMockClient: Client[IO] = Client[IO](_ =>
      cats.effect.Resource.pure {
        org.http4s.Response[IO](org.http4s.Status.Ok).withEntity("invalid json")
      }
    )
    val httpClient = HttpClient[IO](errorMockClient, config, "test-token")

    for {
      result <- httpClient.getRates(List(Rate.Pair(Currency.USD, Currency.JPY)))
    } yield result match {
      case Left(_: AppError.DecodingFailed) => ()
      case other                            => fail(s"Expected DecodingFailed, got: $other")
    }
  }

  test("HttpClient should handle OneFrameApiError responses") {
    val errorMockClient: Client[IO] = Client[IO](_ =>
      cats.effect.Resource.pure {
        org.http4s.Response[IO](org.http4s.Status.Ok).withEntity("""{"error": "Quota reached"}""")
      }
    )
    val httpClient = HttpClient[IO](errorMockClient, config, "test-token")

    for {
      result <- httpClient.getRates(List(Rate.Pair(Currency.USD, Currency.JPY)))
    } yield result match {
      case Left(_: AppError.RateLimited) => ()
      case other                         => fail(s"Expected RateLimited, got: $other")
    }
  }

  test("HttpClient should handle HTTP error responses") {
    val errorMockClient: Client[IO] = Client[IO](_ =>
      cats.effect.Resource.pure {
        org.http4s.Response[IO](org.http4s.Status.InternalServerError).withEntity("Server error")
      }
    )
    val httpClient = HttpClient[IO](errorMockClient, config, "test-token")

    for {
      result <- httpClient.getRates(List(Rate.Pair(Currency.USD, Currency.JPY)))
    } yield result match {
      case Left(_: AppError.UpstreamUnavailable) => ()
      case other                                 => fail(s"Expected UpstreamUnavailable, got: $other")
    }
  }

  test("HttpClient should handle empty response array") {
    val emptyMockClient: Client[IO] = Client[IO](_ =>
      cats.effect.Resource.pure {
        org.http4s.Response[IO](org.http4s.Status.Ok).withEntity("[]")
      }
    )
    val httpClient = HttpClient[IO](emptyMockClient, config, "test-token")

    for {
      result <- httpClient.getRates(List(Rate.Pair(Currency.USD, Currency.JPY)))
    } yield result match {
      case Left(_: AppError.NotFound) => ()
      case other                      => fail(s"Expected NotFound, got: $other")
    }
  }
}
