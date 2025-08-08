package forex.services.rates

import cats.effect.IO
import cats.syntax.parallel._
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.integrations.oneframe.Algebra
import forex.integrations.oneframe.Protocol.GetRateResponse
import forex.integrations.oneframe.errors.OneFrameError
import forex.services.cache.Service
import forex.services.rates.{ Service => RatesService }
import munit.CatsEffectSuite
import java.time.OffsetDateTime
import scala.concurrent.duration._

class ServiceSpec extends CatsEffectSuite {

  val validRate: Rate = Rate(
    pair = Rate.Pair(Currency.USD, Currency.JPY),
    price = Price(BigDecimal(123.45)),
    timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z"))
  )

  val validOneFrameResponse = GetRateResponse(
    List(
      forex.integrations.oneframe.Protocol.ExchangeRate(
        from = "USD",
        to = "JPY",
        bid = BigDecimal(123.40),
        ask = BigDecimal(123.50),
        price = BigDecimal(123.45),
        time_stamp = "2024-08-04T12:34:56Z"
      )
    )
  )

  // Mock success OneFrame client
  val successOneFrameClient: Algebra[IO] = (_: Rate.Pair) => IO.pure(Right(validOneFrameResponse))

  // Mock error OneFrame client
  val errorOneFrameClient: Algebra[IO] = (_: Rate.Pair) => IO.pure(Left(OneFrameError.OneFrameLookupFailed("API Down")))

  test("Service should return rate when OneFrame client succeeds") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)

    for {
      result <- service.get(pair)
      _ <- IO(assert(result.isRight))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.JPY))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(123.45)))
    } yield ()
  }

  test("Service should return error when OneFrame client fails") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](errorOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)

    for {
      result <- service.get(pair)
      _ <- IO(assert(result.isLeft))
    } yield ()
  }

  test("Service should handle different currency pairs") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.EUR, Currency.GBP)

    for {
      result <- service.get(pair)
      _ <- IO(assert(result.isRight))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.EUR))
      _ <- IO(assertEquals(rate.pair.to, Currency.GBP))
    } yield ()
  }

  test("Service should return cached rate on subsequent requests") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)

    for {
      // First request - should hit OneFrame
      result1 <- service.get(pair)
      _ <- IO(assert(result1.isRight))
      rate1 <- IO(result1.toOption.get)

      // Second request - should hit cache
      result2 <- service.get(pair)
      _ <- IO(assert(result2.isRight))
      rate2 <- IO(result2.toOption.get)

      // Both should be identical
      _ <- IO(assertEquals(rate1, rate2))
    } yield ()
  }

  test("Service should handle cache miss and put to cache") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)

    for {
      // Clear cache first
      _ <- cache.clear()

      // Request should miss cache and fetch from OneFrame
      result <- service.get(pair)
      _ <- IO(assert(result.isRight))

      // Verify it was cached using the correct key format
      cachedResult <- cache.get[String, Rate](s"${pair.from}_${pair.to}")
      _ <- IO(assert(cachedResult.isDefined))
    } yield ()
  }

  test("Service should handle concurrent requests for same pair") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)

    for {
      // Clear cache first
      _ <- cache.clear()

      // Concurrent requests
      results <- List
                   .fill(5)(service.get(pair))
                   .parTraverse(identity[IO[Either[forex.services.rates.errors.RatesServiceError, Rate]]])

      // All should succeed
      _ <- IO(assert(results.forall(_.isRight)))

      // All should return same rate (except for timestamp which will be different)
      rates = results.map(_.toOption.get)
      _ <- IO(assert(rates.forall(rate => rate.pair == rates.head.pair && rate.price == rates.head.price)))
    } yield ()
  }
}
