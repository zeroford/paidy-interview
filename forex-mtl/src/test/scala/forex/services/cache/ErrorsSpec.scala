package forex.services.cache

import forex.domain.error.AppError
import munit.FunSuite

import java.io.IOException
import java.util.concurrent.TimeoutException

class ErrorsSpec extends FunSuite {

  test("toAppError with TimeoutException should return UpstreamUnavailable") {
    val operation = "GET"
    val exception = new TimeoutException("Cache timeout")
    val error     = errors.toAppError(operation, exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    val upstreamError = error.asInstanceOf[AppError.UpstreamUnavailable]
    assertEquals(upstreamError.service, "CacheService")
    assert(upstreamError.message.contains("Timeout"))
    assert(upstreamError.message.contains(operation))
  }

  test("toAppError with IOException should return UpstreamUnavailable") {
    val operation = "PUT"
    val exception = new IOException("Cache I/O error")
    val error     = errors.toAppError(operation, exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    val upstreamError = error.asInstanceOf[AppError.UpstreamUnavailable]
    assertEquals(upstreamError.service, "CacheService")
    assert(upstreamError.message.contains("I/O error"))
    assert(upstreamError.message.contains(operation))
  }

  test("toAppError with unknown exception should return UnexpectedError") {
    val operation = "CLEAR"
    val exception = new RuntimeException("Unknown cache error")
    val error     = errors.toAppError(operation, exception)

    assert(error.isInstanceOf[AppError.UnexpectedError])
    error match {
      case e: AppError.UnexpectedError => assertEquals(e.message, "Unexpected cache error")
      case _                           => fail("Expected UnexpectedError")
    }
  }

  test("toAppError should include operation name in message") {
    val timeoutException = new TimeoutException("Timeout")
    val getError         = errors.toAppError("GET", timeoutException)
    val putError         = errors.toAppError("PUT", timeoutException)

    getError match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains("GET"))
      case _                               => fail("Expected UpstreamUnavailable")
    }
    putError match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains("PUT"))
      case _                               => fail("Expected UpstreamUnavailable")
    }
  }
}
