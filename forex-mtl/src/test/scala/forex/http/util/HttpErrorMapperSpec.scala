package forex.http.util

import cats.effect.IO
import forex.programs.rates.errors.Error
import munit.CatsEffectSuite
import io.circe.parser._
import org.http4s.Method
import org.http4s.Status

class HttpErrorMapperSpec extends CatsEffectSuite {

  test("map RateLookupFailed returns 502 BadGateway with correct error message") {
    for {
      resp <- HttpErrorMapper.map[IO](Error.RateLookupFailed("external fail"))
      _ <- IO(assertEquals(resp.status, Status.BadGateway))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("External rate provider failed")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadGateway.code)))
    } yield ()
  }

  test("badRequest returns 400 BadRequest with error details") {
    val details = List("Invalid 'from' parameter", "Invalid 'to' parameter")

    for {
      resp <- HttpErrorMapper.badRequest[IO](details)
      _ <- IO(assertEquals(resp.status, Status.BadRequest))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Invalid query parameters")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadRequest.code)))
      detailsFromJson = json.hcursor.get[Seq[String]]("details").toOption.get
      _ <- IO(assert(details.forall(detailsFromJson.contains)))
    } yield ()
  }

  test("methodNotAllow returns 405 MethodNotAllowed with correct message") {
    for {
      resp <- HttpErrorMapper.methodNotAllow[IO](Method.PUT)
      _ <- IO(assertEquals(resp.status, Status.MethodNotAllowed))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Method PUT not allowed")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.MethodNotAllowed.code)))
    } yield ()
  }
}
