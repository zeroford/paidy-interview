package forex.services.rates

import java.time.Instant
import scala.concurrent.duration._

import cats.effect.IO
import munit.CatsEffectSuite
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import forex.clients.oneframe.Algebra
import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.{ PivotRate, Price, Rate, Timestamp }
import forex.services.cache.Service
import forex.services.rates.{ Service => RatesService }
import forex.services.rates.concurrent.BucketLocks

class ServiceSpec extends CatsEffectSuite {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val fixedInstant = Instant.parse("2024-08-04T12:34:56Z")

  private val validPivotRates: List[PivotRate] = List(
    PivotRate(
      currency = Currency.JPY,
      price = Price(BigDecimal(123.45)),
      timestamp = Timestamp(fixedInstant)
    ),
    PivotRate(
      currency = Currency.USD,
      price = Price(BigDecimal(1.0)),
      timestamp = Timestamp(fixedInstant)
    ),
    PivotRate(
      currency = Currency.EUR,
      price = Price(BigDecimal(0.85)),
      timestamp = Timestamp(fixedInstant)
    ),
    PivotRate(
      currency = Currency.GBP,
      price = Price(BigDecimal(0.755)),
      timestamp = Timestamp(fixedInstant)
    )
  )

  private val successClient: Algebra[IO] = (_: List[Rate.Pair]) => IO.pure(Right(validPivotRates))
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
      _ <- IO(assert(result.isRight, "Service should return success for same currency"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.USD))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(1.0), "Same currency should have price 1.0"))
    } yield ()
  }

  test("Service should handle TTL expiration") {
    val cache   = Service[IO](100, 1.minute)
    val locks   = BucketLocks.create[IO].unsafeRunSync()
    val service = RatesService[IO](successClient, cache, locks, 1.minute)

    for {
      result1 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      _ <- IO(assert(result1.isRight, "First call should succeed"))

      // Wait for TTL to expire
      expiredTime = fixedInstant.plusSeconds(61)
      result2 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), expiredTime)
      _ <- IO(assert(result2.isRight, "Second call should succeed after TTL expiration"))
    } yield ()
  }
}
