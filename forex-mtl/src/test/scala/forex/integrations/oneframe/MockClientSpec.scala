package forex.integrations.oneframe

import cats.effect.IO
import forex.domain.currency.Currency
import forex.domain.rates.Rate
import forex.integrations.oneframe.interpreter.MockClient

import munit.CatsEffectSuite

class MockClientSpec extends CatsEffectSuite {

  val mockClient = MockClient[IO]

  test("MockClient should return successful response for any currency pair") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.JPY))

    for {
      result <- mockClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      response <- IO(result.toOption.get)
      _ <- IO(assert(response.rates.nonEmpty))
      rate <- IO(response.rates.head)
      _ <- IO(assertEquals(rate.from, pairs.head.from.toString))
      _ <- IO(assertEquals(rate.to, pairs.head.to.toString))
      _ <- IO(assert(rate.price > 0))
    } yield ()
  }

  test("MockClient should handle different currency pairs consistently") {
    val pairs = List(
      Rate.Pair(Currency.USD, Currency.EUR),
      Rate.Pair(Currency.GBP, Currency.JPY),
      Rate.Pair(Currency.CAD, Currency.AUD)
    )

    for {
      result <- mockClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      response <- IO(result.toOption.get)
      _ <- IO(assert(response.rates.length == pairs.length))
      _ <- IO(pairs.zip(response.rates).foreach { case (pair, rate) =>
             assertEquals(rate.from, pair.from.toString)
             assertEquals(rate.to, pair.to.toString)
           })
    } yield ()
  }

  test("MockClient should return valid price data") {
    val pairs = List(Rate.Pair(Currency.EUR, Currency.GBP))

    for {
      result <- mockClient.getRates(pairs)
      response <- IO(result.toOption.get)
      rate <- IO(response.rates.head)
      _ <- IO(assert(rate.bid > 0))
      _ <- IO(assert(rate.ask > 0))
      _ <- IO(assert(rate.price > 0))
      _ <- IO(assert(rate.bid <= rate.ask))
      _ <- IO(assert(rate.price >= rate.bid && rate.price <= rate.ask))
    } yield ()
  }

  test("MockClient should return valid timestamp") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.JPY))

    for {
      result <- mockClient.getRates(pairs)
      response <- IO(result.toOption.get)
      rate <- IO(response.rates.head)
      _ <- IO(assert(rate.time_stamp.nonEmpty))
      _ <- IO(assert(rate.time_stamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")))
    } yield ()
  }

  test("MockClient should be deterministic for same pairs") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.EUR))

    for {
      result1 <- mockClient.getRates(pairs)
      result2 <- mockClient.getRates(pairs)
      response1 <- IO(result1.toOption.get)
      response2 <- IO(result2.toOption.get)
      rate1 <- IO(response1.rates.head)
      rate2 <- IO(response2.rates.head)
      _ <- IO(assertEquals(rate1.price, rate2.price))
      _ <- IO(assertEquals(rate1.bid, rate2.bid))
      _ <- IO(assertEquals(rate1.ask, rate2.ask))
    } yield ()
  }

  test("MockClient should handle edge case currencies") {
    val edgePairs = List(
      Rate.Pair(Currency.USD, Currency.USD),
      Rate.Pair(Currency.EUR, Currency.EUR),
      Rate.Pair(Currency.JPY, Currency.JPY)
    )

    for {
      result <- mockClient.getRates(edgePairs)
      _ <- IO(assert(result.isRight))
      response <- IO(result.toOption.get)
      _ <- IO(assert(response.rates.length == edgePairs.length))
      _ <- IO(edgePairs.zip(response.rates).foreach { case (pair, rate) =>
             assertEquals(rate.from, pair.from.toString)
             assertEquals(rate.to, pair.to.toString)
           })
    } yield ()
  }
}
