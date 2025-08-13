package forex.domain.error

import munit.FunSuite

class AppErrorSpec extends FunSuite {

  test("AppError.Validation should contain correct message") {
    val error = AppError.Validation("Invalid currency format")
    assertEquals(error.message, "Invalid currency format")
  }

  test("AppError.NotFound should contain correct message") {
    val error = AppError.NotFound("No rate found")
    assertEquals(error.message, "No rate found")
  }

  test("AppError.CalculationFailed should contain correct message") {
    val error = AppError.CalculationFailed("Rate calculation failed")
    assertEquals(error.message, "Rate calculation failed")
  }

  test("AppError.UpstreamAuthFailed should contain correct service and message") {
    val error = AppError.UpstreamAuthFailed("one-frame", "Authentication failed")
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Authentication failed")
  }

  test("AppError.UpstreamUnavailable should contain correct service and message") {
    val error = AppError.UpstreamUnavailable("one-frame", "Service unavailable")
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Service unavailable")
  }

  test("AppError.RateLimited should contain correct service and message") {
    val error = AppError.RateLimited("one-frame", "Rate limited")
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Rate limited")
  }

  test("AppError.DecodingFailed should contain correct service and message") {
    val error = AppError.DecodingFailed("one-frame", "Failed to decode response")
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Failed to decode response")
  }

  test("AppError.UnexpectedError should contain correct message") {
    val error = AppError.UnexpectedError("Internal server error")
    assertEquals(error.message, "Internal server error")
  }

  test("AppError.BadRequest should contain correct message") {
    val error = AppError.BadRequest("Bad request")
    assertEquals(error.message, "Bad request")
  }

  test("AppError instances should be different types") {
    val validation          = AppError.Validation("test")
    val notFound            = AppError.NotFound("test")
    val calculationFailed   = AppError.CalculationFailed("test")
    val upstreamAuth        = AppError.UpstreamAuthFailed("service", "test")
    val upstreamUnavailable = AppError.UpstreamUnavailable("service", "test")
    val rateLimited         = AppError.RateLimited("service", "test")
    val decodingFailed      = AppError.DecodingFailed("service", "test")
    val unexpected          = AppError.UnexpectedError("test")
    val badRequest          = AppError.BadRequest("test")

    assert(validation.isInstanceOf[AppError.Validation])
    assert(notFound.isInstanceOf[AppError.NotFound])
    assert(calculationFailed.isInstanceOf[AppError.CalculationFailed])
    assert(upstreamAuth.isInstanceOf[AppError.UpstreamAuthFailed])
    assert(upstreamUnavailable.isInstanceOf[AppError.UpstreamUnavailable])
    assert(rateLimited.isInstanceOf[AppError.RateLimited])
    assert(decodingFailed.isInstanceOf[AppError.DecodingFailed])
    assert(unexpected.isInstanceOf[AppError.UnexpectedError])
    assert(badRequest.isInstanceOf[AppError.BadRequest])
  }

  test("AppError should extend Error trait") {
    val error = AppError.Validation("test")
    assert(error.isInstanceOf[Error])
  }
}
