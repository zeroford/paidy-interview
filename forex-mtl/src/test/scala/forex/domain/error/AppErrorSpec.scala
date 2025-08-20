package forex.domain.error

import munit.FunSuite

class AppErrorSpec extends FunSuite {

  test("AppError should preserve error messages") {
    val message           = "Test error message"
    val validation        = AppError.Validation(message)
    val notFound          = AppError.NotFound(message)
    val badRequest        = AppError.BadRequest(message)
    val calculationFailed = AppError.CalculationFailed(message)
    val unexpectedError   = AppError.UnexpectedError(message)

    assertEquals(validation.message, message)
    assertEquals(notFound.message, message)
    assertEquals(badRequest.message, message)
    assertEquals(calculationFailed.message, message)
    assertEquals(unexpectedError.message, message)
  }

  test("AppError service errors should have operation context") {
    val operation = "fetchRates"
    val message   = "Service unavailable"

    val upstreamUnavailable = AppError.UpstreamUnavailable(operation, message)
    val upstreamAuthFailed  = AppError.UpstreamAuthFailed(operation, message)
    val rateLimited         = AppError.RateLimited(operation, message)
    val decodingFailed      = AppError.DecodingFailed(operation, message)

    assertEquals(upstreamUnavailable.service, operation)
    assertEquals(upstreamAuthFailed.service, operation)
    assertEquals(rateLimited.service, operation)
    assertEquals(decodingFailed.service, operation)

    assertEquals(upstreamUnavailable.message, message)
    assertEquals(upstreamAuthFailed.message, message)
    assertEquals(rateLimited.message, message)
    assertEquals(decodingFailed.message, message)
  }

  test("AppError types should be distinct") {
    val message   = "test"
    val operation = "testOp"

    val validation          = AppError.Validation(message)
    val notFound            = AppError.NotFound(message)
    val badRequest          = AppError.BadRequest(message)
    val calculationFailed   = AppError.CalculationFailed(message)
    val unexpectedError     = AppError.UnexpectedError(message)
    val upstreamUnavailable = AppError.UpstreamUnavailable(operation, message)
    val upstreamAuthFailed  = AppError.UpstreamAuthFailed(operation, message)
    val rateLimited         = AppError.RateLimited(operation, message)
    val decodingFailed      = AppError.DecodingFailed(operation, message)

    // Test that they are instances of different types
    assert(validation.isInstanceOf[AppError.Validation])
    assert(notFound.isInstanceOf[AppError.NotFound])
    assert(badRequest.isInstanceOf[AppError.BadRequest])
    assert(calculationFailed.isInstanceOf[AppError.CalculationFailed])
    assert(unexpectedError.isInstanceOf[AppError.UnexpectedError])
    assert(upstreamUnavailable.isInstanceOf[AppError.UpstreamUnavailable])
    assert(upstreamAuthFailed.isInstanceOf[AppError.UpstreamAuthFailed])
    assert(rateLimited.isInstanceOf[AppError.RateLimited])
    assert(decodingFailed.isInstanceOf[AppError.DecodingFailed])
  }

  test("AppError should handle empty strings") {
    val validation = AppError.Validation("")
    val notFound   = AppError.NotFound("")

    assertEquals(validation.message, "")
    assertEquals(notFound.message, "")
  }

  test("AppError should handle special characters") {
    val specialMessage = "Error with special chars: @#$%^&*()"
    val validation     = AppError.Validation(specialMessage)

    assertEquals(validation.message, specialMessage)
  }

  test("AppError should handle long messages") {
    val longMessage =
      "This is a very long error message that contains many words and should be handled properly by the error system without any issues or truncation"
    val unexpectedError = AppError.UnexpectedError(longMessage)

    assertEquals(unexpectedError.message, longMessage)
  }
}
