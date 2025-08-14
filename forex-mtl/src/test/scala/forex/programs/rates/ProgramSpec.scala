package forex.programs.rates

import cats.effect.IO
import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.services.rates.Algebra
import munit.CatsEffectSuite
import java.time.OffsetDateTime

class ProgramSpec extends CatsEffectSuite {

  val validRate: Rate = Rate(
    pair = Rate.Pair(Currency.USD, Currency.JPY),
    price = Price(BigDecimal(123.45)),
    timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z"))
  )

  val successService: Algebra[IO] = (_: Rate.Pair) => IO.pure(Right(validRate))

  val errorService: Algebra[IO] = (_: Rate.Pair) => IO.pure(Left(AppError.UpstreamUnavailable("one-frame", "API Down")))

  test("Program should return rate when service succeeds") {
    val program = Program[IO](successService)
    val request = GetRatesRequest(Currency.USD, Currency.JPY)

    for {
      result <- program.get(request)
      _ <- IO(assert(result.isRight))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.JPY))
    } yield ()
  }

  test("Program should return error when service fails") {
    val program = Program[IO](errorService)
    val request = GetRatesRequest(Currency.USD, Currency.JPY)

    for {
      result <- program.get(request)
      _ <- IO(assert(result.isLeft))
      error <- IO(result.left.toOption.get)
      _ <- IO(assert(error.isInstanceOf[AppError.UpstreamUnavailable]))
    } yield ()
  }

  test("Program should return rate 1.0 for same currency") {
    val program = Program[IO](errorService) // Even with error service, same currency should work
    val request = GetRatesRequest(Currency.USD, Currency.USD)

    for {
      result <- program.get(request)
      _ <- IO(assert(result.isRight))
      rate <- IO(result.toOption.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.USD))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(1.0)))
    } yield ()
  }

}
