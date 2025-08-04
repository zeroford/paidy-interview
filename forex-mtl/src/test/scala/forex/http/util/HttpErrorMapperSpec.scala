package forex.http.util

import cats.effect.IO
import forex.programs.rates.errors.Error
import io.circe.parser._
import org.http4s.Method
import org.http4s.Status
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class HttpErrorMapperSpec extends AnyFunSuite with Matchers {

  test("map RateLookupFailed returns 502 BadGateway with correct error message") {
    val resp = HttpErrorMapper.map[IO](Error.RateLookupFailed("external fail")).unsafeRunSync()
    resp.status shouldBe Status.BadGateway

    val json = parse(resp.as[String].unsafeRunSync()).toOption.get
    json.hcursor.get[String]("message").toOption.value should include("External rate provider failed")
    json.hcursor.get[Int]("code").toOption shouldBe Some(Status.BadGateway.code)
  }

  test("badRequest returns 400 BadRequest with error details") {
    val details = List("Invalid 'from' parameter", "Invalid 'to' parameter")
    val resp    = HttpErrorMapper.badRequest[IO](details).unsafeRunSync()
    resp.status shouldBe Status.BadRequest

    val json = parse(resp.as[String].unsafeRunSync()).toOption.get
    json.hcursor.get[String]("message").toOption.value should include("Invalid query parameters")
    json.hcursor.get[Int]("code").toOption shouldBe Some(Status.BadRequest.code)
    json.hcursor.get[Seq[String]]("details").toOption.value should contain allElementsOf details
  }

  test("methodNotAllow returns 405 MethodNotAllowed with correct message") {
    val resp = HttpErrorMapper.methodNotAllow[IO](Method.PUT).unsafeRunSync()
    resp.status shouldBe Status.MethodNotAllowed

    val json = parse(resp.as[String].unsafeRunSync()).toOption.get
    json.hcursor.get[String]("message").toOption.value should include("Method PUT not allowed")
    json.hcursor.get[Int]("code").toOption shouldBe Some(Status.MethodNotAllowed.code)
  }
}
