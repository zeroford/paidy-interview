package forex.modules

import cats.effect.IO
import io.circe.parser._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import forex.modules.middleware.ErrorHandlerMiddleware

class ErrorHandlerMiddlewareSpec extends CatsEffectSuite {

  test("returns response from inner route unchanged when no error") {
    val inner = HttpRoutes.of[IO] { case GET -> Root / "ok" => Ok("success") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    for {
      resp <- app.run(Request[IO](Method.GET, uri"/ok"))
      body <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Ok)
      assertEquals(body, "success")
    }
  }

  test("maps thrown exceptions to 500 with JSON body") {
    val inner = HttpRoutes.of[IO] { case GET -> Root / "boom" => IO.raiseError(new RuntimeException("boom")) }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    for {
      resp <- app.run(Request[IO](Method.GET, uri"/boom"))
      body <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.InternalServerError)
      val json = parse(body).getOrElse(fail("invalid json"))
      val code = json.hcursor.get[Int]("code").toOption
      val msg  = json.hcursor.get[String]("message").toOption
      assertEquals(code, Some(Status.InternalServerError.code))
      assert(msg.exists(_.nonEmpty))
    }
  }
}
