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
      println(s"Mock client received URI: $uri")
      if (uri.contains("pair=")) {
        val allPairs = uri.split("pair=").drop(1).map(_.split("&")(0)).toList
        println(s"All pairs: $allPairs")
        if (allPairs.isEmpty) {
          println("Returning empty array")
          org.http4s.Response[IO](org.http4s.Status.Ok).withEntity("[]")
        } else {
          val pairList = allPairs
          println(s"Pair list: $pairList")
          val rates = pairList
            .map { pair =>
              val from = pair.take(3)
              val to   = pair.drop(3)
              s"""{"from":"$from","to":"$to","bid":0.0085,"ask":0.0086,"price":0.00855,"time_stamp":"2023-01-01T00:00:00.000000Z"}"""
            }
            .mkString("[", ",", "]")
          println(s"Returning rates: $rates")
          org.http4s.Response[IO](org.http4s.Status.Ok).withEntity(rates)
        }
      } else {
        println("No pairs parameter, returning empty array")
        org.http4s.Response[IO](org.http4s.Status.Ok).withEntity("[]")
      }
    }
  )

  test("HttpClient should handle successful response") {
    val pairs      = List(Rate.Pair(Currency.USD, Currency.JPY))
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      firstRate <- IO(rates.head)
      _ <- IO(assertEquals(firstRate.currency, Currency.JPY))
      _ <- IO(assert(firstRate.price.value > 0))
    } yield ()
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
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assertEquals(rates.size, 3))
      _ <- IO(assert(rates.forall(_.price.value > 0)))
    } yield ()
  }

  test("HttpClient should handle EUR/GBP pair") {
    val pairs      = List(Rate.Pair(Currency.EUR, Currency.GBP))
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      firstRate <- IO(rates.head)
      _ <- IO(assertEquals(firstRate.currency, Currency.GBP))
      _ <- IO(assert(firstRate.price.value > 0))
    } yield ()
  }

  test("HttpClient should handle USD/EUR pair") {
    val pairs      = List(Rate.Pair(Currency.USD, Currency.EUR))
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      firstRate <- IO(rates.head)
      _ <- IO(assertEquals(firstRate.currency, Currency.EUR))
      _ <- IO(assert(firstRate.price.value > 0))
    } yield ()
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
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      _ <- IO(assert(rates.forall(_.price.value > 0)))
    } yield ()
  }

  test("HttpClient should handle different configurations") {
    val pairs           = List(Rate.Pair(Currency.USD, Currency.JPY))
    val differentConfig = config.copy(host = "different-host", port = 8082)
    val differentToken  = "different-token"
    val httpClient      = HttpClient[IO](mockClient, differentConfig, differentToken)

    for {
      result <- httpClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      firstRate <- IO(rates.head)
      _ <- IO(assertEquals(firstRate.currency, Currency.JPY))
      _ <- IO(assert(firstRate.price.value > 0))
    } yield ()
  }

  test("HttpClient should handle empty pairs list") {
    val pairs      = List.empty[Rate.Pair]
    val httpClient = HttpClient[IO](mockClient, config, "test-token")

    for {
      result <- httpClient.getRates(pairs)
      _ <- IO(assert(result.isLeft, "Empty pairs should return error"))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[AppError.NotFound], "Should be NotFound error"))
    } yield ()
  }
}
