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
import forex.services.cache.{ Algebra => CacheAlgebra, Service }
import forex.services.rates.{ Service => RatesService }
import forex.services.rates.concurrent.BucketLocks

class ServiceSpec extends CatsEffectSuite {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val fixedInstant = Instant.parse("2024-08-04T12:34:56Z")

  private val validPivotRates: List[PivotRate] = List(
    PivotRate(Currency.JPY, Price(BigDecimal(123.45)), Timestamp(fixedInstant)),
    PivotRate(Currency.USD, Price(BigDecimal(1.0)), Timestamp(fixedInstant)),
    PivotRate(Currency.EUR, Price(BigDecimal(0.85)), Timestamp(fixedInstant)),
    PivotRate(Currency.GBP, Price(BigDecimal(0.755)), Timestamp(fixedInstant))
  )

  private val successClient: Algebra[IO] = (_: List[Rate.Pair]) => IO.pure(Right(validPivotRates))
  private val errorClient: Algebra[IO]   = (_: List[Rate.Pair]) =>
    IO.pure(Left(AppError.UpstreamUnavailable("one-frame", "API Down")))

  test("Service should return rate when OneFrame client succeeds") {
    val cache = Service[IO](100, 1.minute)

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](successClient, cache, locks, 1.minute)
      result <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
    } yield result match {
      case Right(rate) =>
        assertEquals(rate.pair.from, Currency.USD)
        assertEquals(rate.pair.to, Currency.JPY)
        assertEquals(rate.price.value, BigDecimal(123.45))
      case Left(e) =>
        fail(s"Unexpected error: $e")
    }
  }

  test("Service should return error when OneFrame client fails") {
    val cache = Service[IO](100, 1.minute)

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](errorClient, cache, locks, 1.minute)
      result <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
    } yield result match {
      case Left(_: AppError.UpstreamUnavailable) => ()
      case Left(e)                               => fail(s"Expected UpstreamUnavailable, but got: $e")
      case Right(v)                              => fail(s"Expected error, but got success: $v")
    }
  }

  test("Service should handle different currency pairs") {
    val cache = Service[IO](100, 1.minute)

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](successClient, cache, locks, 1.minute)
      result <- service.get(Rate.Pair(Currency.EUR, Currency.GBP), fixedInstant)
    } yield result match {
      case Right(rate) =>
        assertEquals(rate.pair.from, Currency.EUR)
        assertEquals(rate.pair.to, Currency.GBP)
      case other =>
        fail(s"Unexpected result: $other")
    }
  }

  test("Service should handle cache miss and put to cache") {
    val cache = Service[IO](100, 1.minute)

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](successClient, cache, locks, 1.minute)
      result1 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      result2 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
    } yield (result1, result2) match {
      case (Right(rate1), Right(rate2)) =>
        assertEquals(rate1.price.value, rate2.price.value)
      case (a, b) =>
        fail(s"Unexpected results: $a, $b")
    }
  }

  test("Service should handle same currency pair") {
    val cache = Service[IO](100, 1.minute)

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](successClient, cache, locks, 1.minute)
      result <- service.get(Rate.Pair(Currency.USD, Currency.USD), fixedInstant)
    } yield result match {
      case Right(rate) =>
        assertEquals(rate.pair.from, Currency.USD)
        assertEquals(rate.pair.to, Currency.USD)
        assertEquals(rate.price.value, BigDecimal(1.0))
      case other =>
        fail(s"Unexpected result: $other")
    }
  }

  test("Service should handle TTL expiration") {
    val cache = Service[IO](100, 1.minute)

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](successClient, cache, locks, 1.minute)
      result1 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
      expired = fixedInstant.plusSeconds(61)
      result2 <- service.get(Rate.Pair(Currency.USD, Currency.JPY), expired)
    } yield (result1, result2) match {
      case (Right(_), Right(_)) => ()
      case (a, b)               => fail(s"Unexpected results around TTL: $a, $b")
    }
  }

  test("Service should handle cache error scenarios") {
    val failingCache = new CacheAlgebra[IO] {
      override def get[K, V](key: K): IO[AppError Either Option[V]] =
        IO.pure(Left(AppError.UnexpectedError("Cache error")))
      override def put[K, V](key: K, value: V): IO[AppError Either Unit] =
        IO.pure(Left(AppError.UnexpectedError("Cache error")))
    }

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](successClient, failingCache, locks, 1.minute)
      result <- service.get(Rate.Pair(Currency.EUR, Currency.GBP), fixedInstant)
    } yield result match {
      case Left(_: AppError.UnexpectedError) => ()
      case Left(e)                           => fail(s"Expected UnexpectedError, but got: $e")
      case Right(v)                          => fail(s"Expected error, but got success: $v")
    }
  }

  test("Service should handle missing rates in response") {
    val incompleteClient: Algebra[IO] = (_: List[Rate.Pair]) =>
      IO.pure(Right(List(PivotRate(Currency.USD, Price(BigDecimal(1.0)), Timestamp(fixedInstant)))))

    val cache = Service[IO](100, 1.minute)

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](incompleteClient, cache, locks, 1.minute)
      result <- service.get(Rate.Pair(Currency.USD, Currency.EUR), fixedInstant)
    } yield result match {
      case Left(_: AppError.NotFound) => ()
      case Left(e)                    => fail(s"Expected NotFound, but got: $e")
      case Right(v)                   => fail(s"Expected error, but got success: $v")
    }
  }

  test("Service should handle cache put failures") {
    val cacheWithPutFailure = new CacheAlgebra[IO] {
      override def get[K, V](key: K): IO[AppError Either Option[V]] =
        IO.pure(Right(None))
      override def put[K, V](key: K, value: V): IO[AppError Either Unit] =
        IO.pure(Left(AppError.UnexpectedError("Cache put failed")))
    }

    for {
      locks <- BucketLocks.create[IO]
      service = RatesService[IO](successClient, cacheWithPutFailure, locks, 1.minute)
      result <- service.get(Rate.Pair(Currency.USD, Currency.JPY), fixedInstant)
    } yield result match {
      case Left(_: AppError.UnexpectedError) => ()
      case Left(e)                           => fail(s"Expected UnexpectedError when cache put fails, but got: $e")
      case Right(v)                          => fail(s"Expected error, but got success: $v")
    }
  }
}
