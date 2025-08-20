package forex.http.rates

import cats.effect.IO
import cats.implicits._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._

import forex.domain.error.AppError
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.programs.rates.Algebra
import io.circe.parser.parse
import java.time.Instant

class RatesRoutesSpec extends CatsEffectSuite {

  private val validRate = Rate(
    pair = Rate.Pair(forex.domain.currency.Currency.USD, forex.domain.currency.Currency.JPY),
    price = Price(BigDecimal(123.45)),
    timestamp = Timestamp(Instant.now)
  )

  private val successProgram = new Algebra[IO] {
    override def get(pair: Rate.Pair): IO[AppError Either Rate] =
      IO.pure(Right(validRate))
  }

  private val failureProgram = new Algebra[IO] {
    override def get(pair: Rate.Pair): IO[AppError Either Rate] =
      IO.pure(Left(AppError.CalculationFailed("Rate calculation failed")))
  }

  private val sameCurrencyProgram = new Algebra[IO] {
    override def get(pair: Rate.Pair): IO[AppError Either Rate] =
      if (pair.from == pair.to) IO.pure(Right(Rate(pair, Price(BigDecimal(1.0)), Timestamp(Instant.now))))
      else IO.pure(Right(validRate))
  }

  test("GET /rates should return rate when program succeeds") {
    val routes  = new RatesRoutes[IO](successProgram)
    val request = Request[IO](Method.GET, uri"/rates?from=USD&to=JPY")

    for {
      response <- routes.routes.orNotFound.run(request)
      body <- response.as[String]
    } yield {
      assertEquals(response.status, Status.Ok)
      assert(body.contains("USD"), "Response should contain USD")
      assert(body.contains("JPY"), "Response should contain JPY")
    }
  }

  test("GET /rates should return error when program fails") {
    val routes  = new RatesRoutes[IO](failureProgram)
    val request = Request[IO](Method.GET, uri"/rates?from=USD&to=JPY")

    for {
      response <- routes.routes.orNotFound.run(request)
      body <- response.as[String]
    } yield {
      assertEquals(response.status, Status.UnprocessableEntity)
      assert(body.contains("422"), "Response should contain status code")
    }
  }

  test("GET /rates should handle missing and invalid query parameters") {
    val routes = new RatesRoutes[IO](successProgram)

    val invalidRequests = List(
      Request[IO](Method.GET, uri"/rates"),
      Request[IO](Method.GET, uri"/rates?from=INVALID&to=USD"),
      Request[IO](Method.GET, uri"/rates?from=USD&to=INVALID"),
      Request[IO](Method.GET, uri"/rates?from=&to=USD"),
      Request[IO](Method.GET, uri"/rates?from=USD&to="),
      Request[IO](Method.GET, uri"/rates?from=USD"),
      Request[IO](Method.GET, uri"/rates?to=USD")
    )

    invalidRequests.traverse_ { request =>
      routes.routes.orNotFound.run(request).map { response =>
        assertEquals(response.status, Status.BadRequest)
      }
    }
  }

  test("GET /rates should handle same currency pair") {
    val routes  = new RatesRoutes[IO](sameCurrencyProgram)
    val request = Request[IO](Method.GET, uri"/rates?from=USD&to=USD")

    for {
      response <- routes.routes.orNotFound.run(request)
      bodyStr <- response.as[String]
    } yield {
      assertEquals(response.status, Status.Ok)
      val json     = parse(bodyStr).getOrElse(fail("Response is not valid JSON"))
      val priceOpt = json.hcursor.get[BigDecimal]("price").toOption
      priceOpt match {
        case Some(price) => assertEquals(price, BigDecimal(1.0), "Response should contain price 1.0 for same currency")
        case None        => fail("Missing 'price' field")
      }
    }
  }

  test("GET /rates should handle case insensitive parameters and special characters") {
    val routes   = new RatesRoutes[IO](successProgram)
    val requests = List(
      Request[IO](Method.GET, uri"/rates?from=usd&to=eur"),
      Request[IO](Method.GET, uri"/rates?from=USD&to=EUR&extra=test")
    )

    requests.traverse_ { request =>
      routes.routes.orNotFound.run(request).map { response =>
        assertEquals(response.status, Status.Ok)
      }
    }
  }

  test("GET /rates should handle non-GET methods and non-existent routes") {
    val routes  = new RatesRoutes[IO](successProgram)
    val methods = List(Method.POST, Method.PUT, Method.DELETE, Method.PATCH)

    methods.traverse_ { method =>
      val request = Request[IO](method, uri"/rates?from=USD&to=JPY")
      routes.routes.orNotFound.run(request).map { response =>
        assertEquals(response.status, Status.MethodNotAllowed, s"Failed for method $method")
      }
    } >> {
      val nonExistentRequest = Request[IO](Method.GET, uri"/nonexistent")
      routes.routes.orNotFound.run(nonExistentRequest).map { response =>
        assertEquals(response.status, Status.NotFound)
      }
    }
  }
}
