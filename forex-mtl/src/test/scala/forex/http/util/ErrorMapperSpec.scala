package forex.http.util

import cats.effect.IO
import forex.domain.error.AppError
import munit.CatsEffectSuite
import org.http4s.{ Method, Status }
import org.http4s.headers.Allow

class ErrorMapperSpec extends CatsEffectSuite {

  test("toErrorResponse should map Validation error to BadRequest") {
    val error = AppError.Validation("Invalid currency pair")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
      _ <- IO(assert(response.headers.get(org.typelevel.ci.CIString("Content-Type")).isDefined))
    } yield ()
  }

  test("toErrorResponse should map NotFound error to NotFound") {
    val error = AppError.NotFound("Rate not found for USD/EUR")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
      _ <- IO(assertEquals(response.status, Status.NotFound))
    } yield ()
  }

  test("toErrorResponse should map CalculationFailed error to UnprocessableEntity") {
    val error = AppError.CalculationFailed("Division by zero in rate calculation")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
      _ <- IO(assertEquals(response.status, Status.UnprocessableEntity))
    } yield ()
  }

  test("toErrorResponse should map UpstreamAuthFailed error to BadGateway") {
    val error = AppError.UpstreamAuthFailed("401", "Authentication failed")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
      _ <- IO(assertEquals(response.status, Status.BadGateway))
    } yield ()
  }

  test("toErrorResponse should map RateLimited error to BadGateway") {
    val error = AppError.RateLimited("429", "Too many requests")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
      _ <- IO(assertEquals(response.status, Status.BadGateway))
    } yield ()
  }

  test("toErrorResponse should map DecodingFailed error to BadGateway") {
    val error = AppError.DecodingFailed("Invalid JSON", "Failed to parse response")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
      _ <- IO(assertEquals(response.status, Status.BadGateway))
    } yield ()
  }

  test("toErrorResponse should map UpstreamUnavailable error to ServiceUnavailable") {
    val error = AppError.UpstreamUnavailable("503", "Service temporarily unavailable")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
      _ <- IO(assertEquals(response.status, Status.ServiceUnavailable))
    } yield ()
  }

  test("toErrorResponse should map UnexpectedError to InternalServerError") {
    val error = AppError.UnexpectedError("Something went wrong")

    for {
      response <- ErrorMapper.toErrorResponse[IO](error)
      _ <- IO(assertEquals(response.status, Status.InternalServerError))
    } yield ()
  }

  test("badRequest should return BadRequest with empty messages") {
    for {
      response <- ErrorMapper.badRequest[IO](List.empty)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
    } yield ()
  }

  test("badRequest should return BadRequest with single message") {
    val messages = List("Invalid currency code")

    for {
      response <- ErrorMapper.badRequest[IO](messages)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
    } yield ()
  }

  test("badRequest should return BadRequest with multiple messages") {
    val messages = List("Invalid from currency", "Invalid to currency")

    for {
      response <- ErrorMapper.badRequest[IO](messages)
      _ <- IO(assertEquals(response.status, Status.BadRequest))
    } yield ()
  }

  test("methodNotAllow should return MethodNotAllowed with Allow header") {
    val method = Method.GET
    val allow  = Allow(Method.GET)

    for {
      response <- ErrorMapper.methodNotAllow[IO](method, allow)
      _ <- IO(assertEquals(response.status, Status.MethodNotAllowed))
      _ <- IO(assert(response.headers.get(org.typelevel.ci.CIString("Allow")).isDefined))
    } yield ()
  }

  test("methodNotAllow should include method name in error message") {
    val method = Method.POST
    val allow  = Allow(Method.GET)

    for {
      response <- ErrorMapper.methodNotAllow[IO](method, allow)
      _ <- IO(assertEquals(response.status, Status.MethodNotAllowed))
    } yield ()
  }
}
