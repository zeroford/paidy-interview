package forex.clients.oneframe

import forex.domain.error.AppError
import munit.FunSuite
import java.io.IOException
import java.net.{ ConnectException, SocketTimeoutException }
import java.util.concurrent.TimeoutException

class OneFrameErrorsSpec extends FunSuite {

  test("toAppError with SocketTimeoutException should return UpstreamUnavailable") {
    val exception = new SocketTimeoutException("Connection timeout")
    val error     = errors.toAppError(exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    assertEquals(error.service, "one-frame")
    assert(error.message.contains("Timeout"))
  }

  test("toAppError with TimeoutException should return UpstreamUnavailable") {
    val exception = new TimeoutException("Request timeout")
    val error     = errors.toAppError(exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    assertEquals(error.service, "one-frame")
    assert(error.message.contains("Timeout"))
  }

  test("toAppError with ConnectException should return UpstreamUnavailable") {
    val exception = new ConnectException("Connection refused")
    val error     = errors.toAppError(exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    assertEquals(error.service, "one-frame")
    assert(error.message.contains("Unavailable"))
  }

  test("toAppError with IOException should return UpstreamUnavailable") {
    val exception = new IOException("Network error")
    val error     = errors.toAppError(exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    assertEquals(error.service, "one-frame")
    assert(error.message.contains("Unavailable"))
  }

  test("toAppError with unknown exception should return UnexpectedError") {
    val exception = new RuntimeException("Unknown error")
    val error     = errors.toAppError(exception)

    assert(error.isInstanceOf[AppError.UnexpectedError])
    assert(error.message.contains("Unexpected upstream error"))
  }

  test("toAppError with 'Invalid Currency Pair' should return Validation") {
    val error = errors.toAppError("Invalid Currency Pair")

    assert(error.isInstanceOf[AppError.Validation])
    assertEquals(error.message, "Invalid currency pair")
  }

  test("toAppError with 'No currency pair provided' should return Validation") {
    val error = errors.toAppError("No currency pair provided")

    assert(error.isInstanceOf[AppError.Validation])
    assertEquals(error.message, "Invalid currency pair")
  }

  test("toAppError with 'Quota reached' should return RateLimited") {
    val error = errors.toAppError("Quota reached")

    assert(error.isInstanceOf[AppError.RateLimited])
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Rate limited")
  }

  test("toAppError with 'Rate limited' should return RateLimited") {
    val error = errors.toAppError("Rate limited")

    assert(error.isInstanceOf[AppError.RateLimited])
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Rate limited")
  }

  test("toAppError with 'Forbidden' should return UpstreamAuthFailed") {
    val error = errors.toAppError("Forbidden")

    assert(error.isInstanceOf[AppError.UpstreamAuthFailed])
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Upstream service authentication failed")
  }

  test("toAppError with 'Empty Rate' should return NotFound") {
    val error = errors.toAppError("Empty Rate")

    assert(error.isInstanceOf[AppError.NotFound])
    assertEquals(error.message, "No rate found")
  }

  test("toAppError with 'No Rate Found' should return NotFound") {
    val error = errors.toAppError("No Rate Found")

    assert(error.isInstanceOf[AppError.NotFound])
    assertEquals(error.message, "No rate found")
  }

  test("toAppError with unknown string should return UnexpectedError") {
    val error = errors.toAppError("Unknown error")

    assert(error.isInstanceOf[AppError.UnexpectedError])
    assertEquals(error.message, "Unexpected error")
  }

  test("toAppError with status 400 should return BadRequest") {
    val error = errors.toAppError(400)

    assert(error.isInstanceOf[AppError.BadRequest])
    assertEquals(error.message, "Bad request")
  }

  test("toAppError with status 401 should return UpstreamAuthFailed") {
    val error = errors.toAppError(401)

    assert(error.isInstanceOf[AppError.UpstreamAuthFailed])
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Upstream service authentication failed")
  }

  test("toAppError with status 403 should return UpstreamAuthFailed") {
    val error = errors.toAppError(403)

    assert(error.isInstanceOf[AppError.UpstreamAuthFailed])
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Upstream service authentication failed")
  }

  test("toAppError with status 404 should return NotFound") {
    val error = errors.toAppError(404)

    assert(error.isInstanceOf[AppError.NotFound])
    assertEquals(error.message, "No rate found")
  }

  test("toAppError with status 429 should return RateLimited") {
    val error = errors.toAppError(429)

    assert(error.isInstanceOf[AppError.RateLimited])
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Rate limited")
  }

  test("toAppError with status 500 should return UpstreamUnavailable") {
    val error = errors.toAppError(500)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Upstream error 500")
  }

  test("toAppError with status 503 should return UpstreamUnavailable") {
    val error = errors.toAppError(503)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Upstream error 503")
  }

  test("toAppError with unknown status should return UnexpectedError") {
    val error = errors.toAppError(999)

    assert(error.isInstanceOf[AppError.UnexpectedError])
    assertEquals(error.message, "Unexpected error")
  }
}
