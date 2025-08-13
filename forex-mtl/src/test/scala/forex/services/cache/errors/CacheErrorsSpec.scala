package forex.services.cache

import forex.domain.error.AppError
import munit.FunSuite
import java.io.IOException
import java.util.concurrent.TimeoutException

class CacheErrorsSpec extends FunSuite {

  test("toAppError with TimeoutException should return UpstreamUnavailable") {
    val exception = new TimeoutException("Cache timeout")
    val error     = errors.toAppError("GET", exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    assertEquals(error.service, "cache")
    assert(error.message.contains("GET -Timeout"))
  }

  test("toAppError with IOException should return UpstreamUnavailable") {
    val exception = new IOException("Cache I/O error")
    val error     = errors.toAppError("PUT", exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    assertEquals(error.service, "cache")
    assert(error.message.contains("PUT -I/O error"))
  }

  test("toAppError with unknown exception should return UnexpectedError") {
    val exception = new RuntimeException("Unknown cache error")
    val error     = errors.toAppError("CLEAR", exception)

    assert(error.isInstanceOf[AppError.UnexpectedError])
    assertEquals(error.message, "Unexpected cache error")
  }

  test("toAppError should include operation name in message") {
    val timeoutException = new TimeoutException("Timeout")
    val getError         = errors.toAppError("GET", timeoutException)
    val putError         = errors.toAppError("PUT", timeoutException)
    val clearError       = errors.toAppError("CLEAR", timeoutException)

    assert(getError.message.contains("GET -Timeout"))
    assert(putError.message.contains("PUT -Timeout"))
    assert(clearError.message.contains("CLEAR -Timeout"))
  }

  test("toAppError should preserve exception message") {
    val exception = new TimeoutException("Custom timeout message")
    val error     = errors.toAppError("GET", exception)

    assert(error.message.contains("Custom timeout message"))
  }
}
