package forex.http.health

import cats.effect.IO
import io.circe.parser._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._

class HealthRoutesSpec extends CatsEffectSuite {

  test("GET /health should return 200 OK with correct JSON response") {
    val healthRoutes = new HealthRoutes[IO].routes
    val request      = Request[IO](Method.GET, uri"/health")

    for {
      response <- healthRoutes.orNotFound.run(request)
      _ <- IO(assertEquals(response.status, Status.Ok))
      body <- response.as[String]
      json = parse(body).getOrElse(fail("Response is not valid JSON"))
      _ <- IO(assertEquals(json.hcursor.get[String]("status").toOption, Some("OK")))
    } yield ()
  }
}
