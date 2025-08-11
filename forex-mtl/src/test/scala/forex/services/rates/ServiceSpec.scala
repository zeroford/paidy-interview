package forex.services.rates

import cats.effect.IO

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
      ),
      forex.integrations.oneframe.Protocol.ExchangeRate(
        from = "USD",
        to = "USD",
        bid = BigDecimal(1.0),
        ask = BigDecimal(1.0),
        price = BigDecimal(1.0),
        time_stamp = "2024-08-04T12:34:56Z"
      ),
      forex.integrations.oneframe.Protocol.ExchangeRate(
        from = "USD",
        to = "EUR",
        bid = BigDecimal(0.85),
        ask = BigDecimal(0.86),
        price = BigDecimal(0.855),
        time_stamp = "2024-08-04T12:34:56Z"
      ),
      forex.integrations.oneframe.Protocol.ExchangeRate(
        from = "USD",
        to = "GBP",
        bid = BigDecimal(0.75),
        ask = BigDecimal(0.76),
        price = BigDecimal(0.755),
        time_stamp = "2024-08-04T12:34:56Z"
      ),
      forex.integrations.oneframe.Protocol.ExchangeRate(
        from = "EUR",
        to = "USD",
        bid = BigDecimal(1.17),
        ask = BigDecimal(1.18),
        price = BigDecimal(1.175),
        time_stamp = "2024-08-04T12:34:56Z"
      ),
      forex.integrations.oneframe.Protocol.ExchangeRate(
        from = "GBP",
        to = "USD",
        bid = BigDecimal(1.32),
        ask = BigDecimal(1.33),
        price = BigDecimal(1.325),
        time_stamp = "2024-08-04T12:34:56Z"
      )
    )
  )

  // Mock success OneFrame client
  val successOneFrameClient: Algebra[IO] = (_: List[Rate.Pair]) => IO.pure(Right(validOneFrameResponse))

  // Mock error OneFrame client
  val errorOneFrameClient: Algebra[IO] = (_: List[Rate.Pair]) =>
    IO.pure(Left(OneFrameError.OneFrameLookupFailed("API Down")))

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
      _ <- IO(assertEquals(rate.price.value, BigDecimal(1.0) / BigDecimal(123.45)))
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
      cachedResult <- cache.get[String, Rate](s"${pair.from}${pair.to}")
      _ <- IO(assert(cachedResult.isDefined))
    } yield ()
  }

}
