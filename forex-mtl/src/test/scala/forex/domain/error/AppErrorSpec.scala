package forex.domain.error

import munit.FunSuite

class AppErrorSpec extends FunSuite {

  test("AppError should handle validation errors correctly") {
    val error = AppError.Validation("Invalid currency format")
    assertEquals(error.message, "Invalid currency format")
  }

  test("AppError should handle service errors with service name") {
    val error = AppError.UpstreamUnavailable("one-frame", "Service unavailable")
    assertEquals(error.service, "one-frame")
    assertEquals(error.message, "Service unavailable")
  }

  test("AppError should handle different error types distinctly") {
    val validation = AppError.Validation("test")
    val notFound   = AppError.NotFound("test")
    val upstream   = AppError.UpstreamUnavailable("service", "test")

    assert(validation.isInstanceOf[AppError.Validation])
    assert(notFound.isInstanceOf[AppError.NotFound])
    assert(upstream.isInstanceOf[AppError.UpstreamUnavailable])
  }
}
