package forex.services.cache

import java.io.IOException
import java.util.concurrent.TimeoutException

import munit.FunSuite

import forex.domain.error.AppError

class ErrorsSpec extends FunSuite {

  test("toAppError with TimeoutException should return UpstreamUnavailable") {
    val timeoutException = new TimeoutException("Request timed out")
    val result           = errors.toAppError("test-operation", timeoutException)

    assert(result.isInstanceOf[AppError.UpstreamUnavailable])
  }

  test("toAppError with IOException should return UpstreamUnavailable") {
    val ioException = new IOException("Connection failed")
    val result      = errors.toAppError("test-operation", ioException)

    assert(result.isInstanceOf[AppError.UpstreamUnavailable])
  }

  test("toAppError with unknown exception should return UnexpectedError") {
    val unknownException = new RuntimeException("Unknown error")
    val result           = errors.toAppError("test-operation", unknownException)

    assert(result.isInstanceOf[AppError.UnexpectedError])
  }

  test("toAppError should handle different exception types") {
    val nullPointerException     = new NullPointerException("Null pointer")
    val illegalArgumentException = new IllegalArgumentException("Invalid argument")
    val runtimeException         = new RuntimeException("Runtime error")

    val result1 = errors.toAppError("test-operation", nullPointerException)
    val result2 = errors.toAppError("test-operation", illegalArgumentException)
    val result3 = errors.toAppError("test-operation", runtimeException)

    assert(result1.isInstanceOf[AppError.UnexpectedError])
    assert(result2.isInstanceOf[AppError.UnexpectedError])
    assert(result3.isInstanceOf[AppError.UnexpectedError])
  }
}
