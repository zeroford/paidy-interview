package forex.programs.rates

import cats.effect.IO
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.RateProgramError
import forex.services.rates.Algebra
import forex.services.rates.errors.RatesServiceError
import munit.CatsEffectSuite
import java.time.OffsetDateTime

class ProgramSpec extends CatsEffectSuite {

  val validRate: Rate = Rate(
    pair = Rate.Pair(Currency.USD, Currency.JPY),
    price = Price(BigDecimal(123.45)),
    timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z"))
  )

  // Mock success service
  val successService: Algebra[IO] = (_: Rate.Pair) => IO.pure(Right(validRate))

  // Mock error service
  val errorService: Algebra[IO] = (_: Rate.Pair) => IO.pure(Left(RatesServiceError.OneFrameLookupFailed("API Down")))

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
      _ <- IO(assert(error.isInstanceOf[RateProgramError.RateLookupFailed]))
    } yield ()
  }


} 