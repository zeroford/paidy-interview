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

  private val successService: Algebra[IO] =
    (_, _) => IO.pure(Right(validRate))

  private val errorService: Algebra[IO] =
    (_, _) => IO.pure(Left(AppError.UpstreamUnavailable("one-frame", "API Down")))

  test("Program should return rate when service succeeds") {
    val program = Program[IO](successService)

    for {
      result <- program.get(Rate.Pair(Currency.USD, Currency.JPY))
    } yield result match {
      case Right(rate) =>
        assertEquals(rate.pair.from, Currency.USD)
        assertEquals(rate.pair.to, Currency.JPY)
        assertEquals(rate.price.value, BigDecimal(123.45))
      case Left(e) =>
        fail(s"Unexpected error: $e")
    }
  }

  test("Program should return error when service fails") {
    val program = Program[IO](errorService)

    for {
      result <- program.get(Rate.Pair(Currency.USD, Currency.JPY))
    } yield result match {
      case Left(_: AppError.UpstreamUnavailable) => ()
      case Left(e)                               => fail(s"Expected UpstreamUnavailable, but got: $e")
      case Right(v)                              => fail(s"Expected error, but got success: $v")
    }
  }

  test("Program should return rate 1.0 for same currency") {
    val program = Program[IO](errorService)

    for {
      result <- program.get(Rate.Pair(Currency.USD, Currency.USD))
    } yield result match {
      case Right(rate) =>
        assertEquals(rate.pair.from, Currency.USD)
        assertEquals(rate.pair.to, Currency.USD)
        assertEquals(rate.price.value, BigDecimal(1.0))
      case other =>
        fail(s"Unexpected result: $other")
    }
  }

  test("Program should handle different currency pairs for same currency") {
    val program = Program[IO](errorService)

    for {
      result1 <- program.get(Rate.Pair(Currency.EUR, Currency.EUR))
      result2 <- program.get(Rate.Pair(Currency.JPY, Currency.JPY))
      result3 <- program.get(Rate.Pair(Currency.GBP, Currency.GBP))
    } yield {
      result1 match {
        case Right(rate1) => assertEquals(rate1.price.value, BigDecimal(1.0))
        case other        => fail(s"EUR/EUR unexpected: $other")
      }
      result2 match {
        case Right(rate2) => assertEquals(rate2.price.value, BigDecimal(1.0))
        case other        => fail(s"JPY/JPY unexpected: $other")
      }
      result3 match {
        case Right(rate3) => assertEquals(rate3.price.value, BigDecimal(1.0))
        case other        => fail(s"GBP/GBP unexpected: $other")
      }
    }
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
    } yield {
      result1 match {
        case Left(_: AppError.Validation) => ()
        case other                        => fail(s"Expected Validation error, got: $other")
      }
      result2 match {
        case Left(_: AppError.NotFound) => ()
        case other                      => fail(s"Expected NotFound error, got: $other")
      }
      result3 match {
        case Left(_: AppError.CalculationFailed) => ()
        case other                               => fail(s"Expected CalculationFailed error, got: $other")
      }
    }
  }
}
