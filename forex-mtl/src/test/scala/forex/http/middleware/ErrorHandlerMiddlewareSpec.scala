package forex.http.middleware

import cats.effect.IO
import munit.CatsEffectSuite
import io.circe.parser._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

class ErrorHandlerMiddlewareSpec extends CatsEffectSuite {

  test("returns response from inner route unchanged when no error") {
    val inner = HttpRoutes.of[IO] { case GET -> Root / "ok" => Ok("success") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, uri"/ok")
    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.Ok))
      body <- resp.as[String]
      _ <- IO(assertEquals(body, "success"))
    } yield ()
  }

  test("returns 404 and JSON body when no matching route") {
    val inner = HttpRoutes.empty[IO]
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, uri"/notfound")
    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.NotFound))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.NotFound.code)))
      message = json.hcursor.get[String]("message").toOption.get
      _ <- IO(assert(message.contains("/notfound")))
    } yield ()
  }

  test("returns 500 and JSON error body on exception") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("boom!") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, uri"/crash")
    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.InternalServerError))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.InternalServerError.code)))
      message = json.hcursor.get[String]("message").toOption.get
      _ <- IO(assert(message.contains("boom")))
    } yield ()
  }
}
