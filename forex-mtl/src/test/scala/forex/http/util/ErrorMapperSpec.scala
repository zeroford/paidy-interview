package forex.http.util

import cats.effect.IO
import forex.domain.error.AppError
import munit.CatsEffectSuite
import io.circe.parser._
import org.http4s.Method
import org.http4s.Status

class ErrorMapperSpec extends CatsEffectSuite {

  test("toErrorResponse Validation returns 400 BadRequest") {
    for {
      response <- ErrorMapper.toErrorResponse[IO](AppError.Validation("Invalid currency format"))
      _ <- IO(assertEquals(response.status, Status.BadRequest))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Invalid currency format")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadRequest.code)))
    } yield ()
  }

  test("toErrorResponse NotFound returns 404 NotFound") {
    for {
      response <- ErrorMapper.toErrorResponse[IO](AppError.NotFound("No rate found"))
      _ <- IO(assertEquals(response.status, Status.NotFound))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("No rate found")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.NotFound.code)))
    } yield ()
  }

  test("toErrorResponse CalculationFailed returns 422 UnprocessableEntity") {
    for {
      response <- ErrorMapper.toErrorResponse[IO](AppError.CalculationFailed("Rate calculation failed"))
      _ <- IO(assertEquals(response.status, Status.UnprocessableEntity))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Rate calculation failed")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.UnprocessableEntity.code)))
    } yield ()
  }

  test("toErrorResponse UpstreamAuthFailed returns 502 BadGateway") {
    for {
      response <- ErrorMapper.toErrorResponse[IO](AppError.UpstreamAuthFailed("one-frame", "Authentication failed"))
      _ <- IO(assertEquals(response.status, Status.BadGateway))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Authentication failed")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadGateway.code)))
    } yield ()
  }

  test("toErrorResponse RateLimited returns 502 BadGateway") {
    for {
      response <- ErrorMapper.toErrorResponse[IO](AppError.RateLimited("one-frame", "Rate limited"))
      _ <- IO(assertEquals(response.status, Status.BadGateway))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Rate limited")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadGateway.code)))
    } yield ()
  }

  test("toErrorResponse DecodingFailed returns 502 BadGateway") {
    for {
      response <- ErrorMapper.toErrorResponse[IO](AppError.DecodingFailed("one-frame", "Failed to decode response"))
      _ <- IO(assertEquals(response.status, Status.BadGateway))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Failed to decode response")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadGateway.code)))
    } yield ()
  }

  test("toErrorResponse UpstreamUnavailable returns 503 ServiceUnavailable") {
    for {
      response <- ErrorMapper.toErrorResponse[IO](AppError.UpstreamUnavailable("one-frame", "Service unavailable"))
      _ <- IO(assertEquals(response.status, Status.ServiceUnavailable))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Service unavailable")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.ServiceUnavailable.code)))
    } yield ()
  }

  test("toErrorResponse UnexpectedError returns 500 InternalServerError") {
    for {
      response <- ErrorMapper.toErrorResponse[IO](AppError.UnexpectedError("Internal server error"))
      _ <- IO(assertEquals(response.status, Status.InternalServerError))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Internal server error")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.InternalServerError.code)))
    } yield ()
  }

  test("badRequest returns 400 BadRequest with error details") {
    val details = List("Invalid 'from' parameter", "Invalid 'to' parameter")

    for {
      response <- ErrorMapper.badRequest[IO](details)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Invalid query parameters")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.BadRequest.code)))
    } yield ()
  }

  test("methodNotAllow returns 405 MethodNotAllowed with correct message") {
    for {
      response <- ErrorMapper.methodNotAllow[IO](Method.PUT)
      _ <- IO(assertEquals(response.status, Status.MethodNotAllowed))
      bodyStr <- response.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assert(json.hcursor.get[String]("message").toOption.get.contains("Method PUT not allowed")))
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.MethodNotAllowed.code)))
    } yield ()
  }
}
