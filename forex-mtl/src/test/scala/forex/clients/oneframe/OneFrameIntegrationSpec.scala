package forex.clients.oneframe

import cats.effect.IO
import cats.effect.Resource
import forex.config.OneFrameConfig
import forex.domain.currency.Currency
import forex.domain.rates.Rate
import forex.clients.oneframe.interpreter.HttpClient
import forex.clients.oneframe.errors.OneFrameError
import munit.CatsEffectSuite
import org.http4s.client.Client
import org.http4s.{ Request, Response, Status }
import org.http4s.circe.CirceEntityCodec._
import io.circe.Json

class OneFrameIntegrationSpec extends CatsEffectSuite {

  val config = OneFrameConfig(
    host = "localhost",
    port = 8081
  )

  val testToken = "test-token"
  val testPair  = Rate.Pair(Currency.USD, Currency.JPY)

  test("OneFrame integration should handle successful rate lookup") {
    val mockResponse = Response[IO](Status.Ok)
      .withEntity(
        Json.arr(
          Json.obj(
            "from" -> Json.fromString("USD"),
            "to" -> Json.fromString("JPY"),
            "bid" -> Json.fromBigDecimal(BigDecimal(123.40)),
            "ask" -> Json.fromBigDecimal(BigDecimal(123.50)),
            "price" -> Json.fromBigDecimal(BigDecimal(123.45)),
            "time_stamp" -> Json.fromString("2024-08-04T12:34:56Z")
          )
        )
      )

    val mockClient: Client[IO] = Client[IO] { _ =>
      Resource.pure(mockResponse)
    }

    val httpClient = HttpClient[IO](mockClient, config, testToken)

    for {
      result <- httpClient.getRates(List(testPair))
      _ <- IO(assert(result.isRight))
      response <- IO(result.toOption.get)
      _ <- IO(assert(response.rates.nonEmpty))
      rate <- IO(response.rates.head)
      _ <- IO(assertEquals(rate.from, "USD"))
      _ <- IO(assertEquals(rate.to, "JPY"))
      _ <- IO(assertEquals(rate.price, BigDecimal(123.45)))
    } yield ()
  }

  test("OneFrame integration should handle HTTP error responses") {
    val mockResponse = Response[IO](Status.BadGateway)
      .withEntity("Service unavailable")

    val mockClient: Client[IO] = Client[IO] { _ =>
      Resource.pure(mockResponse)
    }

    val httpClient = HttpClient[IO](mockClient, config, testToken)

    for {
      result <- httpClient.getRates(List(testPair))
      _ <- IO(assert(result.isLeft))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[OneFrameError.UnknownError]))
      unknownError <- IO(error.asInstanceOf[OneFrameError.UnknownError])
      _ <- IO(assert(unknownError.e != null))
    } yield ()
  }

  test("OneFrame integration should handle empty response array") {
    val mockResponse = Response[IO](Status.Ok)
      .withEntity(Json.arr())

    val mockClient: Client[IO] = Client[IO] { _ =>
      Resource.pure(mockResponse)
    }

    val httpClient = HttpClient[IO](mockClient, config, testToken)

    for {
      result <- httpClient.getRates(List(testPair))
      _ <- IO(assert(result.isLeft))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[OneFrameError.UnknownError]))
      unknownError <- IO(error.asInstanceOf[OneFrameError.UnknownError])
      _ <- IO(assert(unknownError.e != null))
    } yield ()
  }

  test("OneFrame integration should include authentication header") {
    var capturedRequest: Option[Request[IO]] = None

    val mockClient: Client[IO] = Client[IO] { req =>
      capturedRequest = Some(req)
      Resource.pure(Response[IO](Status.Ok).withEntity(Json.arr()))
    }

    val httpClient = HttpClient[IO](mockClient, config, testToken)

    for {
      _ <- httpClient.getRates(List(testPair))
      request <- IO(capturedRequest.get)
      _ <- IO(assert(request.headers.get(org.typelevel.ci.CIString("token")).isDefined))
      _ <- IO(assertEquals(request.headers.get(org.typelevel.ci.CIString("token")).get.head.value, "test-token"))
    } yield ()
  }

  test("OneFrame integration should build correct URI with query parameters") {
    var capturedRequest: Option[Request[IO]] = None

    val mockClient: Client[IO] = Client[IO] { req =>
      capturedRequest = Some(req)
      Resource.pure(Response[IO](Status.Ok).withEntity(Json.arr()))
    }

    val httpClient = HttpClient[IO](mockClient, config, testToken)

    for {
      _ <- httpClient.getRates(List(testPair))
      request <- IO(capturedRequest.get)
      _ <- IO(assertEquals(request.method.name, "GET"))
      _ <- IO(assert(request.uri.path.toString == "/rates"))
      _ <- IO(assert(request.uri.query.params.get("pair").contains("USDJPY")))
    } yield ()
  }

  test("OneFrame integration should handle malformed JSON response") {
    val mockResponse = Response[IO](Status.Ok)
      .withEntity("invalid json")

    val mockClient: Client[IO] = Client[IO] { _ =>
      Resource.pure(mockResponse)
    }

    val httpClient = HttpClient[IO](mockClient, config, testToken)

    for {
      result <- httpClient.getRates(List(testPair))
      _ <- IO(assert(result.isLeft))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[OneFrameError.UnknownError]))
      unknownError <- IO(error.asInstanceOf[OneFrameError.UnknownError])
      _ <- IO(assert(unknownError.e != null))
    } yield ()
  }

  test("OneFrame integration should handle different currency pairs") {
    val mockResponse = Response[IO](Status.Ok)
      .withEntity(
        Json.arr(
          Json.obj(
            "from" -> Json.fromString("EUR"),
            "to" -> Json.fromString("GBP"),
            "bid" -> Json.fromBigDecimal(BigDecimal(0.85)),
            "ask" -> Json.fromBigDecimal(BigDecimal(0.86)),
            "price" -> Json.fromBigDecimal(BigDecimal(0.855)),
            "time_stamp" -> Json.fromString("2024-08-04T12:34:56Z")
          )
        )
      )

    val mockClient: Client[IO] = Client[IO] { _ =>
      Resource.pure(mockResponse)
    }

    val httpClient = HttpClient[IO](mockClient, config, testToken)
    val eurGbpPair = Rate.Pair(Currency.EUR, Currency.GBP)

    for {
      result <- httpClient.getRates(List(eurGbpPair))
      _ <- IO(assert(result.isRight))
      response <- IO(result.toOption.get)
      _ <- IO(assert(response.rates.nonEmpty))
      rate <- IO(response.rates.head)
      _ <- IO(assertEquals(rate.from, "EUR"))
      _ <- IO(assertEquals(rate.to, "GBP"))
      _ <- IO(assertEquals(rate.price, BigDecimal(0.855)))
    } yield ()
  }
}
