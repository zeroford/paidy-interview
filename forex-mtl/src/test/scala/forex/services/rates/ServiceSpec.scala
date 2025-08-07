package forex.services.rates

import cats.effect.IO
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.integrations.oneframe.Algebra
import forex.integrations.oneframe.Protocol.GetRateResponse
import forex.integrations.oneframe.errors.OneFrameError
import forex.services.rates.errors.RatesServiceError
import munit.CatsEffectSuite
import java.time.OffsetDateTime

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
    val service = Service[IO](successOneFrameClient)
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
    val service = Service[IO](errorOneFrameClient)
    val pair    = Rate.Pair(Currency.USD, Currency.JPY)

    for {
      result <- service.get(pair)
      _ <- IO(assert(result.isLeft))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[RatesServiceError.OneFrameLookupFailed]))
    } yield ()
  }

  test("Service should handle different currency pairs") {
    val service = Service[IO](successOneFrameClient)
    val pair    = Rate.Pair(Currency.EUR, Currency.GBP)

    for {
      result <- service.get(pair)
      _ <- IO(assert(result.isRight))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.EUR))
      _ <- IO(assertEquals(rate.pair.to, Currency.GBP))
    } yield ()
  }
}
