package forex.clients.oneframe

import cats.effect.IO
import munit.CatsEffectSuite
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import forex.clients.oneframe.interpreter.MockClient
import forex.domain.currency.Currency
import forex.domain.rates.Rate

class MockClientSpec extends CatsEffectSuite {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val mockClient = MockClient[IO]

  test("MockClient should return valid rates for USD/JPY pair") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.JPY))

    for {
      result <- mockClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      firstRate <- IO(rates.head)
      _ <- IO(assertEquals(firstRate.from, "USD"))
      _ <- IO(assertEquals(firstRate.to, "JPY"))
      _ <- IO(assert(firstRate.price > 0))
    } yield ()
  }

  test("MockClient should return valid rates for multiple pairs") {
    val pairs = List(
      Rate.Pair(Currency.USD, Currency.EUR),
      Rate.Pair(Currency.EUR, Currency.GBP),
      Rate.Pair(Currency.GBP, Currency.JPY)
    )

    for {
      result <- mockClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assertEquals(rates.size, 3))
      _ <- IO(assert(rates.forall(_.price > 0)))
    } yield ()
  }

  test("MockClient should return valid rates for EUR/GBP pair") {
    val pairs = List(Rate.Pair(Currency.EUR, Currency.GBP))

    for {
      result <- mockClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      firstRate <- IO(rates.head)
      _ <- IO(assertEquals(firstRate.from, "EUR"))
      _ <- IO(assertEquals(firstRate.to, "GBP"))
      _ <- IO(assert(firstRate.price > 0))
    } yield ()
  }

  test("MockClient should return valid rates for USD/EUR pair") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.EUR))

    for {
      result <- mockClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      firstRate <- IO(rates.head)
      _ <- IO(assertEquals(firstRate.from, "USD"))
      _ <- IO(assertEquals(firstRate.to, "EUR"))
      _ <- IO(assert(firstRate.price > 0))
    } yield ()
  }

  test("MockClient should return valid rates for edge cases") {
    val edgePairs = List(
      Rate.Pair(Currency.USD, Currency.USD),
      Rate.Pair(Currency.EUR, Currency.EUR),
      Rate.Pair(Currency.GBP, Currency.GBP)
    )

    for {
      result <- mockClient.getRates(edgePairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      _ <- IO(assert(rates.forall(_.price > 0)))
    } yield ()
  }

  test("MockClient should return valid rates for USD/EUR pair with specific validation") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.EUR))

    for {
      result <- mockClient.getRates(pairs)
      _ <- IO(assert(result.isRight))
      rates <- IO(result.toOption.get)
      _ <- IO(assert(rates.nonEmpty))
      firstRate <- IO(rates.head)
      _ <- IO(assertEquals(firstRate.from, "USD"))
      _ <- IO(assertEquals(firstRate.to, "EUR"))
      _ <- IO(assert(firstRate.price > 0))
      _ <- IO(assert(firstRate.price < 1000.0))
    } yield ()
  }
}
