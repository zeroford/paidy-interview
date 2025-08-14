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

  val successOneFrameClient: Algebra[IO] = (_: List[Rate.Pair]) => IO.pure(Right(validOneFrameResponse))

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

  test("Service should handle cache miss and put to cache") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)

    for {
      _ <- cache.clear()

      result <- service.get(pair)
      _ <- IO(assert(result.isRight))

      cachedResult <- cache.get[String, Rate](s"${pair.from}${pair.to}")
      _ <- IO(assert(cachedResult.isDefined))
    } yield ()
  }

  test("Service should use consistent timestamp for USD as base") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.USD, Currency.EUR)

    for {
      _ <- cache.clear()

      result1 <- service.get(pair)
      _ <- IO(assert(result1.isRight))
      rate1 <- IO(result1.toOption.get)

      result2 <- service.get(pair)
      _ <- IO(assert(result2.isRight))
      rate2 <- IO(result2.toOption.get)

      _ <- IO(assertEquals(rate1.timestamp, rate2.timestamp))
      _ <- IO(assertEquals(rate1.pair, Rate.Pair(Currency.USD, Currency.EUR)))
      _ <- IO(assertEquals(rate1.price.value, BigDecimal(0.855)))
    } yield ()
  }

  test("Service should use consistent timestamp for USD as quote") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.EUR, Currency.USD)

    for {
      _ <- cache.clear()

      result1 <- service.get(pair)
      _ <- IO(assert(result1.isRight))
      rate1 <- IO(result1.toOption.get)

      result2 <- service.get(pair)
      _ <- IO(assert(result2.isRight))
      rate2 <- IO(result2.toOption.get)

      _ <- IO(assertEquals(rate1.timestamp, rate2.timestamp))
      _ <- IO(assertEquals(rate1.pair, Rate.Pair(Currency.EUR, Currency.USD)))
      _ <- IO(assertEquals(rate1.price.value, BigDecimal(1.0) / BigDecimal(0.855)))
    } yield ()
  }

  test("Service should handle cross-rate with older timestamp") {
    val cache   = Service[IO](100, 5.minutes)
    val service = RatesService[IO](successOneFrameClient, cache, 5.minutes)
    val pair    = Rate.Pair(Currency.EUR, Currency.GBP)

    for {
      _ <- cache.clear()

      result1 <- service.get(pair)
      _ <- IO(assert(result1.isRight))
      rate1 <- IO(result1.toOption.get)

      result2 <- service.get(pair)
      _ <- IO(assert(result2.isRight))
      rate2 <- IO(result2.toOption.get)

      _ <- IO(assertEquals(rate1.timestamp, rate2.timestamp))
      _ <- IO(assertEquals(rate1.pair, Rate.Pair(Currency.EUR, Currency.GBP)))
      _ <- IO(assertEquals(rate1.price.value, BigDecimal(0.755) / BigDecimal(0.855)))
    } yield ()
  }

}