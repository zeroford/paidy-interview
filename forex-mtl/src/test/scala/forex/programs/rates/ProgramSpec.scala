package forex.programs.rates

import java.time.Instant

import cats.effect.IO
import munit.CatsEffectSuite

import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.{ Price, Rate, Timestamp }
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
      result <- program.get(Rate.Pair(Currency.USD, Currency.JPY))
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
      result <- program.get(Rate.Pair(Currency.USD, Currency.JPY))
      _ <- IO(assert(result.isLeft, "Program should return error"))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[AppError.UpstreamUnavailable], "Should be UpstreamUnavailable error"))
    } yield ()
  }

  test("Program should return rate 1.0 for same currency") {
    val program = Program[IO](errorService)

    for {
      result <- program.get(Rate.Pair(Currency.USD, Currency.USD))
      _ <- IO(assert(result.isRight, "Program should return success for same currency"))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.USD))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(1.0), "Same currency should have price 1.0"))
    } yield ()
  }

  test("Program should handle different currency pairs for same currency") {
    val program = Program[IO](errorService)

    for {
      result1 <- program.get(Rate.Pair(Currency.EUR, Currency.EUR))
      result2 <- program.get(Rate.Pair(Currency.JPY, Currency.JPY))
      result3 <- program.get(Rate.Pair(Currency.GBP, Currency.GBP))
      _ <- IO(assert(result1.isRight, "EUR/EUR should return success"))
      _ <- IO(assert(result2.isRight, "JPY/JPY should return success"))
      _ <- IO(assert(result3.isRight, "GBP/GBP should return success"))
      rate1 <- IO(result1.toOption.get)
      rate2 <- IO(result2.toOption.get)
      rate3 <- IO(result3.toOption.get)
      _ <- IO(assertEquals(rate1.price.value, BigDecimal(1.0), "EUR/EUR should have price 1.0"))
      _ <- IO(assertEquals(rate2.price.value, BigDecimal(1.0), "JPY/JPY should have price 1.0"))
      _ <- IO(assertEquals(rate3.price.value, BigDecimal(1.0), "GBP/GBP should have price 1.0"))
    } yield ()
  }

  test("Program should handle different error types") {
    val validationErrorService: Algebra[IO]  = (_, _) => IO.pure(Left(AppError.Validation("Invalid currency pair")))
    val notFoundErrorService: Algebra[IO]    = (_, _) => IO.pure(Left(AppError.NotFound("Rate not found")))
    val calculationErrorService: Algebra[IO] = (_, _) => IO.pure(Left(AppError.CalculationFailed("Calculation failed")))

    val program1 = Program[IO](validationErrorService)
    val program2 = Program[IO](notFoundErrorService)
    val program3 = Program[IO](calculationErrorService)

    for {
      result1 <- program1.get(Rate.Pair(Currency.USD, Currency.JPY))
      result2 <- program2.get(Rate.Pair(Currency.USD, Currency.JPY))
      result3 <- program3.get(Rate.Pair(Currency.USD, Currency.JPY))
      _ <- IO(assert(result1.isLeft, "Should return validation error"))
      _ <- IO(assert(result2.isLeft, "Should return not found error"))
      _ <- IO(assert(result3.isLeft, "Should return calculation error"))
      error1 <- IO(result1.left.toOption.get)
      error2 <- IO(result2.left.toOption.get)
      error3 <- IO(result3.left.toOption.get)
      _ <- IO(assert(error1.isInstanceOf[AppError.Validation], "Should be Validation error"))
      _ <- IO(assert(error2.isInstanceOf[AppError.NotFound], "Should be NotFound error"))
      _ <- IO(assert(error3.isInstanceOf[AppError.CalculationFailed], "Should be CalculationFailed error"))
    } yield ()
  }
}
