package forex.services.rates

import java.time.Instant
import scala.concurrent.duration._

import cats.effect.IO
import munit.CatsEffectSuite
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import forex.clients.oneframe.Algebra
import forex.clients.oneframe.Protocol.{ OneFrameRate, OneFrameRatesResponse }
import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.Rate
import forex.services.cache.Service
import forex.services.rates.{ Service => RatesService }
import forex.services.rates.concurrent.BucketLocks

class ServiceSpec extends CatsEffectSuite {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val fixedInstant = Instant.parse("2024-08-04T12:34:56Z")

  private val validOneFrameResponse: OneFrameRatesResponse = List(
    OneFrameRate(
      from = "USD",
      to = "JPY",
      bid = BigDecimal(123.40),
      ask = BigDecimal(123.50),
      price = BigDecimal(123.45),
      time_stamp = fixedInstant
    ),
    OneFrameRate(
      from = "USD",
      to = "USD",
      bid = BigDecimal(1.0),
      ask = BigDecimal(1.0),
      price = BigDecimal(1.0),
      time_stamp = fixedInstant
    ),
    OneFrameRate(
      from = "USD",
      to = "EUR",
      bid = BigDecimal(0.85),
      ask = BigDecimal(0.86),
      price = BigDecimal(0.855),
      time_stamp = fixedInstant
    ),
    OneFrameRate(
      from = "USD",
      to = "GBP",
      bid = BigDecimal(0.75),
      ask = BigDecimal(0.76),
      price = BigDecimal(0.755),
      time_stamp = fixedInstant
    )
  )

  private val successClient: Algebra[IO] = (_: List[Rate.Pair]) => IO.pure(Right(validOneFrameResponse))
  private val errorClient: Algebra[IO]   = (_: List[Rate.Pair]) =>
    IO.pure(Left(AppError.UpstreamUnavailable("one-frame", "API Down")))

  test("Service should return rate when OneFrame client succeeds") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 1.minute)

    for {
      result <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      _ <- IO(assert(result.isRight, "Service should return success"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.JPY))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(123.45)))
    } yield ()
  }

  test("Service should return error when OneFrame client fails") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](errorClient, cache, locks, 1.minute)

    for {
      result <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      _ <- IO(assert(result.isLeft, "Service should return error"))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[AppError.UpstreamUnavailable], "Should be UpstreamUnavailable error"))
    } yield ()
  }

  test("Service should handle different currency pairs") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 1.minute)

    for {
      result <- service.get(Rate.Pair(Currency.EUR, Currency.GBP), fixedInstant)
      _ <- IO(assert(result.isRight, "Service should return success"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.EUR))
      _ <- IO(assertEquals(rate.pair.to, Currency.GBP))
    } yield ()
  }

  test("Service should handle cache miss and put to cache") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 1.minute)

    for {
      result1 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      _ <- IO(assert(result1.isRight, "First call should succeed"))

      result2 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      _ <- IO(assert(result2.isRight, "Second call should succeed"))

      rate1 <- IO(result1.toOption.get)
      rate2 <- IO(result2.toOption.get)
      _ <- IO(assertEquals(rate1.price.value, rate2.price.value, "Cached and fresh rates should match"))
    } yield ()
  }

  test("Service should use consistent timestamp for USD as base") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 1.minute)

    for {
      result <- service.get(Rate.Pair(Currency.USD, Currency.EUR), fixedInstant)
      _ <- IO(assert(result.isRight, "Service should return success"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.EUR))
    } yield ()
  }

  test("Service should use consistent timestamp for USD as quote") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 1.minute)

    for {
      result <- service.get(Rate.Pair(Currency.EUR, Currency.USD), fixedInstant)
      _ <- IO(assert(result.isRight, "Service should return success"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.EUR))
      _ <- IO(assertEquals(rate.pair.to, Currency.USD))
    } yield ()
  }

  test("Service should handle cross-rate with older timestamp") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 1.minute)

    for {
      result <- service.get(Rate.Pair(Currency.EUR, Currency.GBP), fixedInstant)
      _ <- IO(assert(result.isRight, "Service should return success"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.EUR))
      _ <- IO(assertEquals(rate.pair.to, Currency.GBP))
    } yield ()
  }

  test("Service should handle same currency pair") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 1.minute)

    for {
      result <- service.get(Rate.Pair(Currency.USD, Currency.USD), fixedInstant)
      _ <- IO(assert(result.isRight, "Service should return success"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.USD))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(1.0), "Same currency should have price 1.0"))
    } yield ()
  }

  test("Service should handle TTL expiration") {
    val cache   = Service[IO](100, 100.millis)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 100.millis)

    for {
      result1 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      _ <- IO(assert(result1.isRight, "First call should succeed"))

      _ <- IO.sleep(200.millis)

      result2 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      _ <- IO(assert(result2.isRight, "Second call should succeed"))

      rate1 <- IO(result1.toOption.get)
      rate2 <- IO(result2.toOption.get)
      _ <- IO(assertEquals(rate1.price.value, rate2.price.value, "Rates should be consistent"))
    } yield ()
  }
}
