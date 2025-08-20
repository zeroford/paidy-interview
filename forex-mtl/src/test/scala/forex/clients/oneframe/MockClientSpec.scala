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

  private val mockClient = MockClient[IO]

  test("MockClient should return valid rates for USD/JPY pair") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.JPY))

    for {
      result <- mockClient.getRates(pairs)
      _         = assert(result.isRight)
      rates     = result.toOption.get
      _         = assert(rates.nonEmpty)
      firstRate = rates.head
      _         = assertEquals(firstRate.currency, Currency.JPY)
      _         = assert(firstRate.price.value > 0)
    } yield ()
  }

  test("MockClient should return valid rates for multiple pairs") {
    val pairs = List(
      Rate.Pair(Currency.SGD, Currency.SGD),
      Rate.Pair(Currency.AUD, Currency.AUD),
      Rate.Pair(Currency.CAD, Currency.CAD)
    )

    for {
      result <- mockClient.getRates(pairs)
      _     = assert(result.isRight)
      rates = result.toOption.get
      _     = assertEquals(rates.size, 3)
      _     = assert(rates.forall(_.price.value > 0))
    } yield ()
  }

  test("MockClient should return valid rates for EUR/GBP pair") {
    val pairs = List(Rate.Pair(Currency.EUR, Currency.GBP))

    for {
      result <- mockClient.getRates(pairs)
      _         = assert(result.isRight)
      rates     = result.toOption.get
      _         = assert(rates.nonEmpty)
      firstRate = rates.head
      _         = assertEquals(firstRate.currency, Currency.GBP)
      _         = assert(firstRate.price.value > 0)
    } yield ()
  }

  test("MockClient should return valid rates for USD/EUR pair") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.EUR))

    for {
      result <- mockClient.getRates(pairs)
      _         = assert(result.isRight)
      rates     = result.toOption.get
      _         = assert(rates.nonEmpty)
      firstRate = rates.head
      _         = assertEquals(firstRate.currency, Currency.EUR)
      _         = assert(firstRate.price.value > 0)
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
      _     = assert(result.isRight)
      rates = result.toOption.get
      _     = assert(rates.nonEmpty)
      _     = assert(rates.forall(_.price.value > 0))
    } yield ()
  }

  test("MockClient should return valid rates for USD/EUR pair with specific validation") {
    val pairs = List(Rate.Pair(Currency.USD, Currency.EUR))

    for {
      result <- mockClient.getRates(pairs)
      _         = assert(result.isRight)
      rates     = result.toOption.get
      _         = assert(rates.nonEmpty)
      firstRate = rates.head
      _         = assertEquals(firstRate.currency, Currency.EUR)
      _         = assert(firstRate.price.value > 0)
      _         = assert(firstRate.price.value < 1000.0)
    } yield ()
  }
}
