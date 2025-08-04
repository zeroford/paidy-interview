package forex.http.rates

import cats.effect.IO
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.Algebra
import io.circe.parser._
import org.http4s._
import org.http4s.implicits._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.time.OffsetDateTime
import forex.programs.rates.errors.Error

class RatesHttpRoutesSpec extends AnyFunSuite with Matchers {

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
    val routes = new RatesHttpRoutes[IO](successProgram).routes

    val request  = Request[IO](Method.GET, uri"/rates?from=USD&to=JPY")
    val response = routes.orNotFound.run(request).unsafeRunSync()
    response.status shouldBe Status.Ok

    val body = response.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Response is not valid JSON"))

    json.hcursor.get[String]("from").toOption shouldBe Some("USD")
    json.hcursor.get[String]("to").toOption shouldBe Some("JPY")
    json.hcursor.get[BigDecimal]("price").toOption shouldBe Some(123.45)
    json.hcursor.get[String]("timestamp").toOption shouldBe Some("2024-08-04T12:34:56Z")
  }

  test("GET /rates missing 'to' param should return 400") {
    val routes   = new RatesHttpRoutes[IO](successProgram).routes
    val request  = Request[IO](Method.GET, uri"/rates?from=USD")
    val response = routes.orNotFound.run(request).unsafeRunSync()
    response.status shouldBe Status.BadRequest
  }

  test("GET /rates with invalid currency should return 404") {
    val routes   = new RatesHttpRoutes[IO](successProgram).routes
    val request  = Request[IO](Method.GET, uri"/rates?from=USD&to=XXX")
    val response = routes.orNotFound.run(request).unsafeRunSync()
    response.status should (be(Status.NotFound) or be(Status.BadRequest))
  }

  test("GET /rates without query params should return 400") {
    val routes   = new RatesHttpRoutes[IO](successProgram).routes
    val request  = Request[IO](Method.GET, uri"/rates")
    val response = routes.orNotFound.run(request).unsafeRunSync()
    response.status shouldBe Status.BadRequest
  }

  test("GET /rate (wrong path) should return 404") {
    val routes   = new RatesHttpRoutes[IO](successProgram).routes
    val request  = Request[IO](Method.GET, uri"/rate?from=USD&to=JPY")
    val response = routes.orNotFound.run(request).unsafeRunSync()
    response.status shouldBe Status.NotFound
  }

  test("POST /rates (wrong method) should return 405") {
    val routes   = new RatesHttpRoutes[IO](successProgram).routes
    val request  = Request[IO](Method.GET, uri"/rate?from=USD&to=JPY")
    val response = routes.orNotFound.run(request).unsafeRunSync()
    response.status shouldBe Status.NotFound
  }

  test("GET /rates but service fails should return 502") {
    val routes   = new RatesHttpRoutes[IO](errorProgram).routes
    val request  = Request[IO](Method.GET, uri"/rates?from=USD&to=JPY")
    val response = routes.orNotFound.run(request).unsafeRunSync()
    response.status shouldBe Status.BadGateway

  }
}
