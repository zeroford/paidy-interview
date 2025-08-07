package forex.http.util

import cats.effect.IO
import forex.programs.rates.errors.RateProgramError
import munit.CatsEffectSuite
import io.circe.parser._
import org.http4s.Method
import org.http4s.Status

class HttpErrorMapperSpec extends CatsEffectSuite {

  test("map RateLookupFailed returns 502 BadGateway with correct error message") {
    for {
      response <- HttpErrorMapper.map[IO](RateProgramError.RateLookupFailed("external fail"))
      _ <- IO(assertEquals(response.status, Status.BadGateway))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("External rate provider failed")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadGateway.code)))
    } yield ()
  }

  test("badRequest returns 400 BadRequest with error details") {
    val details = List("Invalid 'from' parameter", "Invalid 'to' parameter")

    for {
      response <- HttpErrorMapper.badRequest[IO](details)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Invalid query parameters")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadRequest.code)))
      detailsFromJson = json.hcursor.get[Seq[String]]("details").toOption.get
      _ <- IO(assert(details.forall(detailsFromJson.contains)))
    } yield ()
  }

  test("methodNotAllow returns 405 MethodNotAllowed with correct message") {
    for {
      response <- HttpErrorMapper.methodNotAllow[IO](Method.PUT)
      _ <- IO(assertEquals(response.status, Status.MethodNotAllowed))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Method PUT not allowed")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.MethodNotAllowed.code)))
    } yield ()
  }
}
