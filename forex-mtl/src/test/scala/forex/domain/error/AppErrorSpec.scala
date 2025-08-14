package forex.domain.error

import munit.FunSuite

class AppErrorSpec extends FunSuite {

  object TestData {
    def arbitraryMessage: String = "Test message"
    def arbitraryService: String = "test-service"
  }

  def testAppError(error: AppError, expectedMessage: String): Unit =
    error match {
      case e: AppError.Validation        => assertEquals(e.message, expectedMessage)
      case e: AppError.NotFound          => assertEquals(e.message, expectedMessage)
      case e: AppError.CalculationFailed => assertEquals(e.message, expectedMessage)
      case e: AppError.BadRequest        => assertEquals(e.message, expectedMessage)
      case e: AppError.UnexpectedError   => assertEquals(e.message, expectedMessage)
      case _                             => fail(s"Unexpected error type: ${error.getClass}")
    }

  def testServiceAppError(error: AppError, expectedService: String, expectedMessage: String): Unit =
    error match {
      case e: AppError.UpstreamAuthFailed =>
        assertEquals(e.service, expectedService)
        assertEquals(e.message, expectedMessage)
      case e: AppError.UpstreamUnavailable =>
        assertEquals(e.service, expectedService)
        assertEquals(e.message, expectedMessage)
      case e: AppError.RateLimited =>
        assertEquals(e.service, expectedService)
        assertEquals(e.message, expectedMessage)
      case e: AppError.DecodingFailed =>
        assertEquals(e.service, expectedService)
        assertEquals(e.message, expectedMessage)
      case _ =>
        fail(s"Expected service error but got: ${error.getClass}")
    }

  test("AppError.Validation should contain correct message") {
    val error = AppError.Validation("Invalid currency format")
    testAppError(error, "Invalid currency format")
  }

  test("AppError.NotFound should contain correct message") {
    val error = AppError.NotFound("No rate found")
    testAppError(error, "No rate found")
  }

  test("AppError.CalculationFailed should contain correct message") {
    val error = AppError.CalculationFailed("Rate calculation failed")
    testAppError(error, "Rate calculation failed")
  }

  test("AppError.BadRequest should contain correct message") {
    val error = AppError.BadRequest("Bad request")
    testAppError(error, "Bad request")
  }

  test("AppError.UnexpectedError should contain correct message") {
    val error = AppError.UnexpectedError("Internal server error")
    testAppError(error, "Internal server error")
  }

  test("AppError.UpstreamAuthFailed should contain correct service and message") {
    val error = AppError.UpstreamAuthFailed("one-frame", "Authentication failed")
    testServiceAppError(error, "one-frame", "Authentication failed")
  }

  test("AppError.UpstreamUnavailable should contain correct service and message") {
    val error = AppError.UpstreamUnavailable("one-frame", "Service unavailable")
    testServiceAppError(error, "one-frame", "Service unavailable")
  }

  test("AppError.RateLimited should contain correct service and message") {
    val error = AppError.RateLimited("one-frame", "Rate limited")
    testServiceAppError(error, "one-frame", "Rate limited")
  }

  test("AppError.DecodingFailed should contain correct service and message") {
    val error = AppError.DecodingFailed("one-frame", "Failed to decode response")
    testServiceAppError(error, "one-frame", "Failed to decode response")
  }

  test("AppError instances should be different types") {
    val validation          = AppError.Validation("test")
    val notFound            = AppError.NotFound("test")
    val calculationFailed   = AppError.CalculationFailed("test")
    val badRequest          = AppError.BadRequest("test")
    val upstreamAuth        = AppError.UpstreamAuthFailed("service", "test")
    val upstreamUnavailable = AppError.UpstreamUnavailable("service", "test")
    val rateLimited         = AppError.RateLimited("service", "test")
    val decodingFailed      = AppError.DecodingFailed("service", "test")
    val unexpected          = AppError.UnexpectedError("test")

    assert(validation.isInstanceOf[AppError.Validation])
    assert(notFound.isInstanceOf[AppError.NotFound])
    assert(calculationFailed.isInstanceOf[AppError.CalculationFailed])
    assert(badRequest.isInstanceOf[AppError.BadRequest])
    assert(upstreamAuth.isInstanceOf[AppError.UpstreamAuthFailed])
    assert(upstreamUnavailable.isInstanceOf[AppError.UpstreamUnavailable])
    assert(rateLimited.isInstanceOf[AppError.RateLimited])
    assert(decodingFailed.isInstanceOf[AppError.DecodingFailed])
    assert(unexpected.isInstanceOf[AppError.UnexpectedError])
  }

  test("AppError should extend Error trait") {
    val error = AppError.Validation("test")
    assert(error.isInstanceOf[Error])
  }

  test("AppError should handle empty messages") {
    val error = AppError.Validation("")
    assertEquals(error.message, "")
  }

  test("AppError should handle null messages") {
    val error = AppError.Validation(null)
    assertEquals(error.message, null)
  }

  test("AppError should handle special characters in messages") {
    val specialMessage = "Error with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"
    val error          = AppError.Validation(specialMessage)
    assertEquals(error.message, specialMessage)
  }

  test("AppError should handle unicode characters in messages") {
    val unicodeMessage = "Error with unicode: 你好世界 🌍"
    val error          = AppError.Validation(unicodeMessage)
    assertEquals(error.message, unicodeMessage)
  }

  test("AppError should handle very long messages") {
    val longMessage = "A" * 1000
    val error       = AppError.Validation(longMessage)
    assertEquals(error.message, longMessage)
  }

  test("AppError types should be properly classified") {
    val validationErrors = List(
      AppError.Validation("test"),
      AppError.BadRequest("test")
    )

    val notFoundErrors = List(
      AppError.NotFound("test")
    )

    val calculationErrors = List(
      AppError.CalculationFailed("test")
    )

    val upstreamErrors = List(
      AppError.UpstreamAuthFailed("service", "test"),
      AppError.UpstreamUnavailable("service", "test"),
      AppError.RateLimited("service", "test"),
      AppError.DecodingFailed("service", "test")
    )

    val unexpectedErrors = List(
      AppError.UnexpectedError("test")
    )

    validationErrors.foreach { error =>
      assert(error.isInstanceOf[AppError])
      error match {
        case e: AppError.Validation => assert(e.message.nonEmpty)
        case e: AppError.BadRequest => assert(e.message.nonEmpty)
        case _                      => fail("Unexpected error type")
      }
    }

    notFoundErrors.foreach { error =>
      assert(error.isInstanceOf[AppError])
      error match {
        case e: AppError.NotFound => assert(e.message.nonEmpty)
        case _                    => fail("Unexpected error type")
      }
    }

    calculationErrors.foreach { error =>
      assert(error.isInstanceOf[AppError])
      error match {
        case e: AppError.CalculationFailed => assert(e.message.nonEmpty)
        case _                             => fail("Unexpected error type")
      }
    }

    upstreamErrors.foreach { error =>
      assert(error.isInstanceOf[AppError])
      error match {
        case e: AppError.UpstreamAuthFailed =>
          assert(e.message.nonEmpty)
          assert(e.service.nonEmpty)
        case e: AppError.UpstreamUnavailable =>
          assert(e.message.nonEmpty)
          assert(e.service.nonEmpty)
        case e: AppError.RateLimited =>
          assert(e.message.nonEmpty)
          assert(e.service.nonEmpty)
        case e: AppError.DecodingFailed =>
          assert(e.message.nonEmpty)
          assert(e.service.nonEmpty)
        case _ => fail("Unexpected error type")
      }
    }

    unexpectedErrors.foreach { error =>
      assert(error.isInstanceOf[AppError])
      error match {
        case e: AppError.UnexpectedError => assert(e.message.nonEmpty)
        case _                           => fail("Unexpected error type")
      }
    }
  }

  test("AppError messages should be consistent across instances") {
    val message = "Test error message"

    val errors = List(
      AppError.Validation(message),
      AppError.NotFound(message),
      AppError.CalculationFailed(message),
      AppError.BadRequest(message),
      AppError.UnexpectedError(message)
    )

    errors.foreach { error =>
      error match {
        case e: AppError.Validation        => assertEquals(e.message, message)
        case e: AppError.NotFound          => assertEquals(e.message, message)
        case e: AppError.CalculationFailed => assertEquals(e.message, message)
        case e: AppError.BadRequest        => assertEquals(e.message, message)
        case e: AppError.UnexpectedError   => assertEquals(e.message, message)
        case _                             => fail("Unexpected error type")
      }
    }
  }

  test("AppError service names should be consistent across instances") {
    val service = "test-service"
    val message = "Test error message"

    val serviceErrors = List(
      AppError.UpstreamAuthFailed(service, message),
      AppError.UpstreamUnavailable(service, message),
      AppError.RateLimited(service, message),
      AppError.DecodingFailed(service, message)
    )

    serviceErrors.foreach { error =>
      error match {
        case e: AppError.UpstreamAuthFailed  => assertEquals(e.service, service)
        case e: AppError.UpstreamUnavailable => assertEquals(e.service, service)
        case e: AppError.RateLimited         => assertEquals(e.service, service)
        case e: AppError.DecodingFailed      => assertEquals(e.service, service)
        case _                               => fail("Unexpected error type")
      }
    }
  }
}
