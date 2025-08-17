package forex.http.rates

import java.time.Instant

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.{ Method, Request, Status, Uri }
import org.http4s.implicits._

import forex.domain.currency.Currency
import forex.domain.error.AppError
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest

class RatesRoutesSpec extends CatsEffectSuite {

  private val fixedInstant = Instant.parse("2024-08-04T12:34:56Z")
  private val validRate    = Rate(
    pair = Rate.Pair(Currency.USD, Currency.EUR),
    price = Price(BigDecimal(0.85)),
    timestamp = Timestamp(fixedInstant)
  )

  private val successProgram: RatesProgram[IO] = (_: GetRatesRequest) => IO.pure(Right(validRate))
  private val errorProgram: RatesProgram[IO]   = (_: GetRatesRequest) =>
    IO.pure(Left(AppError.UpstreamUnavailable("one-frame", "API Down")))

  test("GET /rates should return rate when program succeeds") {
    val routes  = new RatesRoutes[IO](successProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=EUR"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.Ok))
      body <- response.as[String]
      _ <- IO(assert(body.contains("USD"), "Response should contain USD"))
      _ <- IO(assert(body.contains("EUR"), "Response should contain EUR"))
      _ <- IO(assert(body.contains("0.85"), "Response should contain price"))
    } yield ()
  }

  test("GET /rates should return error when program fails") {
    val routes  = new RatesRoutes[IO](errorProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=EUR"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.ServiceUnavailable))
      body <- response.as[String]
      _ <- IO(assert(body.contains("503"), "Response should contain status code"))
      _ <- IO(assert(body.contains("API Down"), "Response should contain error message"))
    } yield ()
  }

  test("GET /rates should handle missing query parameters") {
    val routes  = new RatesRoutes[IO](successProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
    } yield ()
  }

  test("GET /rates should handle invalid currency") {
    val routes  = new RatesRoutes[IO](successProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=INVALID&to=EUR"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
    } yield ()
  }

  test("GET /rates should handle same currency pair") {
    val routes  = new RatesRoutes[IO](successProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=USD"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
      body <- response.as[String]
      _ <- IO(assert(body.contains("Same currency not allowed"), "Response should contain validation error"))
    } yield ()
  }

  test("GET /rates should handle different currency pairs") {
    val routes  = new RatesRoutes[IO](successProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=EUR&to=GBP"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.Ok))
      body <- response.as[String]
      _ <- IO(assert(body.contains("USD"), "Response should contain USD from mock"))
      _ <- IO(assert(body.contains("EUR"), "Response should contain EUR from mock"))
    } yield ()
  }
}
