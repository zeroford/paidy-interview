package forex.http.middleware

import cats.effect.IO
import io.circe.parser._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ErrorHandlerMiddlewareSpec extends AnyFunSuite with Matchers {

  test("returns response from inner route unchanged when no error") {
    val inner = HttpRoutes.of[IO] { case GET -> Root / "ok" => Ok("success") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req  = Request[IO](Method.GET, uri"/ok")
    val resp = app.run(req).unsafeRunSync()
    resp.status shouldBe Status.Ok
    resp.as[String].unsafeRunSync() shouldBe "success"
  }

  test("returns 404 and JSON body when no matching route") {
    val inner = HttpRoutes.empty[IO]
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req  = Request[IO](Method.GET, uri"/notfound")
    val resp = app.run(req).unsafeRunSync()
    resp.status shouldBe Status.NotFound

    val json = parse(resp.as[String].unsafeRunSync()).toOption.get
    json.hcursor.get[Int]("code").toOption shouldBe Some(Status.NotFound.code)
    json.hcursor.get[String]("message").toOption.value should include("/notfound")
  }

  test("returns 500 and JSON error body on exception") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("boom!") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req  = Request[IO](Method.GET, uri"/crash")
    val resp = app.run(req).unsafeRunSync()
    resp.status shouldBe Status.InternalServerError

    val json = parse(resp.as[String].unsafeRunSync()).toOption.get
    json.hcursor.get[Int]("code").toOption shouldBe Some(Status.InternalServerError.code)
    json.hcursor.get[String]("message").toOption.value should include("boom")
  }
}
