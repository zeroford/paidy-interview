package forex

import scala.concurrent.duration.DurationInt

import cats.effect.{ IO, Resource }
import io.circe.parser.parse
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.client.Client

import forex.config.ClientDefault

class RatesAcceptanceSpec extends CatsEffectSuite {

  private val base =
    Uri
      .fromString(sys.env.getOrElse("APP_BASE_URL", "http://localhost:8080"))
      .fold(throw _, identity)

  private val config = ClientDefault(
    totalTimeout = 2.seconds,
    idleTimeout = 30.seconds,
    maxTotal = 50
  )

  private def clientR: Resource[IO, Client[IO]] =
    forex.modules.HttpClientBuilder.build[IO](config)

  private def GET(c: Client[IO], path: Uri, qs: (String, String)*): IO[Response[IO]] =
    c.run(Request[IO](Method.GET, path.withQueryParams(qs.toMap))).use(IO.pure)

  test("GET /rates JPY→EUR -> 200 OK & fields present") {
    clientR.use { c =>
      GET(c, base / "rates", "from" -> "JPY", "to" -> "EUR").flatMap { res =>
        assertEquals(res.status, Status.Ok)
        res.as[String].flatMap { body =>
          val json = parse(body).fold(throw _, identity)
          val c    = json.hcursor
          assert(c.get[String]("from").contains("JPY"))
          assert(c.get[String]("to").contains("EUR"))
          assert(c.get[BigDecimal]("price").isRight)
          assert(c.get[String]("timestamp").isRight)
          IO.unit
        }
      }
    }
  }

  test("GET /rates USD→USD -> 200 OK & price = 1.0") {
    clientR.use { c =>
      GET(c, base / "rates", "from" -> "USD", "to" -> "USD").flatMap { res =>
        assertEquals(res.status, Status.Ok)
        res.as[String].flatMap { body =>
          val json = parse(body).fold(throw _, identity)
          val c    = json.hcursor
          assert(c.get[String]("from").contains("USD"))
          assert(c.get[String]("to").contains("USD"))
          assertEquals(c.get[BigDecimal]("price").toOption, Some(BigDecimal(1.0)))
          assert(c.get[String]("timestamp").isRight)
          IO.unit
        }
      }
    }
  }

  test("GET /rates missing params -> 400 BadRequest") {
    clientR.use { c =>
      GET(c, base / "rates").map(res => assertEquals(res.status, Status.BadRequest))
    }
  }

  test("POST /rates -> 405  Allow: GET") {
    clientR.use { c =>
      c.run(Request[IO](Method.POST, base / "rates")).use { res =>
        assertEquals(res.status, Status.MethodNotAllowed)
        IO.unit
      }
    }
  }

  test("GET /ratesss (typo) -> 404") {
    clientR.use { c =>
      c.run(Request[IO](Method.GET, base / "ratesss")).use { res =>
        assertEquals(res.status, Status.NotFound)
        IO.unit
      }
    }
  }

  test("GET /rates invalid currency -> 400") {
    clientR.use { c =>
      GET(c, base / "rates", "from" -> "ABC", "to" -> "XYZ").map { res =>
        assertEquals(res.status, Status.BadRequest)
      }
    }
  }

  test("GET /rates empty param -> 400") {
    clientR.use { c =>
      GET(c, base / "rates", "from" -> "", "to" -> "EUR").map { res =>
        assertEquals(res.status, Status.BadRequest)
      }
    }
  }

}
