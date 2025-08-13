package forex.http.rates

import cats.effect.IO
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.Algebra
import forex.domain.error.AppError
import munit.CatsEffectSuite
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import java.time.OffsetDateTime

class RatesHttpRoutesSpec extends CatsEffectSuite {

  val validRate: Rate = Rate(
    pair = Rate.Pair(Currency.EUR, Currency.JPY),
    price = Price(BigDecimal(123.45)),
    timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z"))
  )

  val successProgram: Algebra[IO] = (_: GetRatesRequest) => IO.pure(Right(validRate))

  val errorProgram: Algebra[IO] = (_: GetRatesRequest) =>
    IO.pure(Left(AppError.UpstreamUnavailable("one-frame", "Service unavailable")))

  val validationErrorProgram: Algebra[IO] = (_: GetRatesRequest) =>
    IO.pure(Left(AppError.Validation("Invalid currency pair")))

  val notFoundErrorProgram: Algebra[IO] = (_: GetRatesRequest) => IO.pure(Left(AppError.NotFound("No rate found")))

  test("GET /rates should return 200 and correct JSON") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=EUR&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.Ok))
      body <- response.as[String]
      json = parse(body).getOrElse(fail("Response is not valid JSON"))
      _ <- IO(assertEquals(json.hcursor.get[String]("from").toOption, Some("EUR")))
      _ <- IO(assertEquals(json.hcursor.get[String]("to").toOption, Some("JPY")))
      _ <- IO(assertEquals(json.hcursor.get[BigDecimal]("price").toOption, Some(BigDecimal("123.45"))))
      _ <- IO(assertEquals(json.hcursor.get[String]("timestamp").toOption, Some("2024-08-04T12:34:56Z")))
    } yield ()
  }

  test("GET /rates missing 'to' param should return 400") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=EUR")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
    } yield ()
  }

  test("GET /rates with invalid currency should return 400") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=EUR&to=XXX")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
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
    val request = Request[IO](Method.GET, uri"/rate?from=EUR&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.NotFound))
    } yield ()
  }

  test("POST /rates (wrong method) should return 405") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.POST, uri"/rates?from=EUR&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.MethodNotAllowed))
    } yield ()
  }

  test("GET /rates but service unavailable should return 503") {
    val routes  = new RatesHttpRoutes[IO](errorProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=EUR&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.ServiceUnavailable))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Service unavailable")))
    } yield ()
  }

  test("GET /rates but validation fails should return 400") {
    val routes  = new RatesHttpRoutes[IO](validationErrorProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=EUR&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Invalid currency pair")))
    } yield ()
  }

  test("GET /rates but rate not found should return 404") {
    val routes  = new RatesHttpRoutes[IO](notFoundErrorProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=EUR&to=JPY")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.NotFound))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("No rate found")))
    } yield ()
  }

  test("GET /rates with same currency should return 200") {
    val routes  = new RatesHttpRoutes[IO](successProgram).routes
    val request = Request[IO](Method.GET, uri"/rates?from=EUR&to=EUR")

    for {
      response <- routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.Ok))
      body <- response.as[String]
      json = parse(body).getOrElse(fail("Response is not valid JSON"))
      _ <- IO(assertEquals(json.hcursor.get[String]("from").toOption, Some("EUR")))
      _ <- IO(assertEquals(json.hcursor.get[String]("to").toOption, Some("EUR")))
      _ <- IO(assertEquals(json.hcursor.get[BigDecimal]("price").toOption, Some(BigDecimal("1.0"))))
    } yield ()
  }
}
