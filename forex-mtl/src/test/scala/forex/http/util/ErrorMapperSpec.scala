package forex.http.util

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.{ Method, Status }
import org.http4s.headers.Allow

import forex.domain.error.AppError

class ErrorMapperSpec extends CatsEffectSuite {

  test("toErrorResponse should map Validation error to BadRequest") {
    val error = AppError.Validation("Invalid currency pair")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
    } yield {
      assertEquals(response.status, Status.BadRequest)
      assert(response.headers.get(org.typelevel.ci.CIString("Content-Type")).isDefined)
    }
  }

  test("toErrorResponse should map NotFound error to NotFound") {
    val error = AppError.NotFound("Rate not found for USD/EUR")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
    } yield assertEquals(response.status, Status.NotFound)
  }

  test("toErrorResponse should map CalculationFailed error to UnprocessableEntity") {
    val error = AppError.CalculationFailed("Division by zero in rate calculation")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
    } yield assertEquals(response.status, Status.UnprocessableEntity)
  }

  test("toErrorResponse should map UpstreamAuthFailed error to BadGateway") {
    val error = AppError.UpstreamAuthFailed("401", "Authentication failed")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
    } yield assertEquals(response.status, Status.BadGateway)
  }

  test("toErrorResponse should map RateLimited error to BadGateway") {
    val error = AppError.RateLimited("429", "Too many requests")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
    } yield assertEquals(response.status, Status.BadGateway)
  }

  test("toErrorResponse should map DecodingFailed error to BadGateway") {
    val error = AppError.DecodingFailed("Invalid JSON", "Failed to parse response")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
    } yield assertEquals(response.status, Status.BadGateway)
  }

  test("toErrorResponse should map UpstreamUnavailable error to ServiceUnavailable") {
    val error = AppError.UpstreamUnavailable("503", "Service temporarily unavailable")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
    } yield assertEquals(response.status, Status.ServiceUnavailable)
  }

  test("toErrorResponse should map UnexpectedError to InternalServerError") {
    val error = AppError.UnexpectedError("Something went wrong")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
    } yield assertEquals(response.status, Status.InternalServerError)
  }

  test("badRequest should return BadRequest with empty messages") {
    for {
      response <- ErrorMapper.badRequest[IO](List.empty)
    } yield assertEquals(response.status, Status.BadRequest)
  }

  test("badRequest should return BadRequest with single message") {
    val messages = List("Invalid currency code")

    for {
      response <- ErrorMapper.badRequest[IO](messages)
    } yield assertEquals(response.status, Status.BadRequest)
  }

  test("badRequest should return BadRequest with multiple messages") {
    val messages = List("Invalid from currency", "Invalid to currency")

    for {
      response <- ErrorMapper.badRequest[IO](messages)
    } yield assertEquals(response.status, Status.BadRequest)
  }

  test("methodNotAllow should return MethodNotAllowed with Allow header") {
    val method = Method.GET
    val allow  = Allow(Method.GET)

    for {
      response <- ErrorMapper.methodNotAllow[IO](method, allow)
    } yield {
      assertEquals(response.status, Status.MethodNotAllowed)
      assert(response.headers.get(org.typelevel.ci.CIString("Allow")).isDefined)
    }
  }

  test("methodNotAllow should include method name in error message") {
    val method = Method.POST
    val allow  = Allow(Method.GET)

    for {
      response <- ErrorMapper.methodNotAllow[IO](method, allow)
    } yield assertEquals(response.status, Status.MethodNotAllowed)
  }
}
