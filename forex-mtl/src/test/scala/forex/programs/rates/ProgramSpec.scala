package forex.programs.rates

import java.time.Instant

import cats.effect.IO
import munit.CatsEffectSuite

import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.services.rates.Algebra

class ProgramSpec extends CatsEffectSuite {

  private val validRate = Rate(
    pair = Rate.Pair(Currency.USD, Currency.JPY),
    price = Price(BigDecimal(123.45)),
    timestamp = Timestamp(Instant.parse("2024-08-04T12:34:56Z"))
  )

  private val successService: Algebra[IO] = (_, _) => IO.pure(Right(validRate))
  private val errorService: Algebra[IO] = (_, _) => IO.pure(Left(AppError.UpstreamUnavailable("one-frame", "API Down")))

  test("Program should return rate when service succeeds") {
    val program = Program[IO](successService)

    for {
      result <- program.get(GetRatesRequest(Currency.USD, Currency.JPY))
      _ <- IO(assert(result.isRight, "Program should return success"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.JPY))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(123.45)))
    } yield ()
  }

  test("Program should return error when service fails") {
    val program = Program[IO](errorService)

    for {
      result <- program.get(GetRatesRequest(Currency.USD, Currency.JPY))
      _ <- IO(assert(result.isLeft, "Program should return error"))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[AppError.UpstreamUnavailable], "Should be UpstreamUnavailable error"))
    } yield ()
  }

  test("Program should return rate 1.0 for same currency") {
    val program = Program[IO](errorService)

    for {
      result <- program.get(GetRatesRequest(Currency.USD, Currency.USD))
      _ <- IO(assert(result.isRight, "Program should return success for same currency"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.USD))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(1.0), "Same currency should have price 1.0"))
    } yield ()
  }
}
