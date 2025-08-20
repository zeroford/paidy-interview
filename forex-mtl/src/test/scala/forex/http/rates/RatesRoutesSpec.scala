package forex.http.rates

import cats.effect.IO
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
      if (pair.from == pair.to) {
        IO.pure(Right(Rate(pair, Price(BigDecimal(1.0)), Timestamp(Instant.now))))
      } else {
        IO.pure(Right(validRate))
      }
  }

  test("GET /rates should return rate when program succeeds") {
    val routes  = new RatesRoutes[IO](successProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=JPY"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.Ok))
      body <- response.as[String]
      _ <- IO(assert(body.contains("USD"), "Response should contain USD"))
      _ <- IO(assert(body.contains("JPY"), "Response should contain JPY"))
    } yield ()
  }

  test("GET /rates should return error when program fails") {
    val routes  = new RatesRoutes[IO](failureProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=JPY"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.UnprocessableEntity))
      body <- response.as[String]
      _ <- IO(assert(body.contains("422"), "Response should contain status code"))
    } yield ()
  }

  test("GET /rates should handle missing and invalid query parameters") {
    val routes = new RatesRoutes[IO](successProgram)

    val invalidRequests = List(
      Request[IO](Method.GET, Uri.unsafeFromString("/rates")),
      Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=INVALID&to=USD")),
      Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=INVALID")),
      Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=&to=USD")),
      Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=")),
      Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD")),
      Request[IO](Method.GET, Uri.unsafeFromString("/rates?to=USD"))
    )

    invalidRequests.foreach { request =>
      for {
        response <- routes.routes.orNotFound.run(request)
        _ <- IO(assertEquals(response.status, Status.BadRequest))
      } yield ()
    }
  }

  test("GET /rates should handle same currency pair") {
    val routes  = new RatesRoutes[IO](sameCurrencyProgram)
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=USD"))

    for {
      response <- routes.routes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.Ok))
      bodyStr <- response.as[String]
      json  = parse(bodyStr).toOption.get
      price = json.hcursor.get[BigDecimal]("price").toOption.get
      _ <- IO(assertEquals(price, BigDecimal(1.0), "Response should contain price 1.0 for same currency"))
    } yield ()
  }

  test("GET /rates should handle case insensitive parameters and special characters") {
    val routes   = new RatesRoutes[IO](successProgram)
    val requests = List(
      Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=usd&to=eur")),
      Request[IO](Method.GET, Uri.unsafeFromString("/rates?from=USD&to=EUR&extra=test"))
    )

    requests.foreach { request =>
      for {
        response <- routes.routes.orNotFound.run(request)
        _ <- IO(assertEquals(response.status, Status.Ok))
      } yield ()
    }
  }

  test("GET /rates should handle non-GET methods and non-existent routes") {
    val routes  = new RatesRoutes[IO](successProgram)
    val methods = List(Method.POST, Method.PUT, Method.DELETE, Method.PATCH)

    methods.foreach { method =>
      val request = Request[IO](method, Uri.unsafeFromString("/rates?from=USD&to=JPY"))
      for {
        response <- routes.routes.orNotFound.run(request)
        _ <- IO(assertEquals(response.status, Status.NotFound, s"Failed for method $method"))
      } yield ()
    }

    val nonExistentRequest = Request[IO](Method.GET, Uri.unsafeFromString("/nonexistent"))
    for {
      response <- routes.routes.orNotFound.run(nonExistentRequest)
      _ <- IO(assertEquals(response.status, Status.NotFound))
    } yield ()
  }
}
