package forex.http.rates

import cats.effect.IO
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.Algebra
import forex.programs.rates.errors.Error
import munit.CatsEffectSuite
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import java.time.OffsetDateTime

class RatesHttpRoutesSpec extends CatsEffectSuite {

  val validRate: Rate = Rate(
    pair = Rate.Pair(Currency.USD, Currency.JPY),
    price = Price(BigDecimal(123.45)),
    timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z"))
  )

  // Mock success
  val successProgram: Algebra[IO] = (_: GetRatesRequest) => IO.pure(Right(validRate))

  // Mock error
  val errorProgram: Algebra[IO] = (_: GetRatesRequest) => IO.pure(Left(Error.RateLookupFailed("API Down")))

  test("GET /rates should return 200 and correct JSON") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=USD&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.Ok))
      body <- response.as[String]
      json = parse(body).getOrElse(fail("Response is not valid JSON"))
      _ <- IO(assertEquals(json.hcursor.get[String]("from").toOption, Some("USD")))
      _ <- IO(assertEquals(json.hcursor.get[String]("to").toOption, Some("JPY")))
      // _ <- IO(assertEquals(json.hcursor.get[BigDecimal]("price").toOption, Some(123.45)))
      _ <- IO(assertEquals(json.hcursor.get[String]("timestamp").toOption, Some("2024-08-04T12:34:56Z")))
    } yield ()
  }

  test("GET /rates missing 'to' param should return 400") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=USD")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
    } yield ()
  }

  test("GET /rates with invalid currency should return 404 or 400") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=USD&to=XXX")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assert(response.status == Status.NotFound || response.status == Status.BadRequest))
    } yield ()
  }

  test("GET /rates without query params should return 400") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rates")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
    } yield ()
  }

  test("GET /rate (wrong path) should return 404") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rate?from=USD&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.NotFound))
    } yield ()
  }

  test("POST /rates (wrong method) should return 405") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.POST, uri"/rates?from=USD&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.MethodNotAllowed))
    } yield ()
  }

  test("GET /rates but service fails should return 502") {
    val routes  = new RatesHttpRoutes[IO](errorProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=USD&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadGateway))
    } yield ()
  }
}
