package forex.services.cache

import forex.domain.error.AppError
import munit.FunSuite
import java.io.IOException
import java.util.concurrent.TimeoutException

class CacheErrorsSpec extends FunSuite {

  object TestData {
    def arbitraryTimeoutException: TimeoutException =
      new TimeoutException("Test timeout")

    def arbitraryIOException: IOException =
      new IOException("Test IO")

    def arbitraryRuntimeException: RuntimeException =
      new RuntimeException("Test runtime")

    def arbitraryOperation: String = "GET"
  }

  def testExceptionMapping(
      operation: String,
      exception: Throwable,
      expectedService: String,
      expectedMessageContains: String
  ): Unit = {
    val error = errors.toAppError(operation, exception)
    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    val upstreamError = error.asInstanceOf[AppError.UpstreamUnavailable]
    assertEquals(upstreamError.service, expectedService)
    assert(upstreamError.message.contains(expectedMessageContains))
    assert(upstreamError.message.contains(operation))
  }

  def testOperationInclusion(operation: String, exception: Throwable): Unit = {
    val error = errors.toAppError(operation, exception)
    error match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains(operation))
      case e: AppError.UnexpectedError     => assert(e.message.contains(operation))
      case _                               => fail("Unexpected error type")
    }
  }

  test("toAppError with TimeoutException should return UpstreamUnavailable") {
    val operation = "GET"
    val exception = new TimeoutException("Cache timeout")
    testExceptionMapping(operation, exception, "cache", "Timeout")
  }

  test("toAppError with IOException should return UpstreamUnavailable") {
    val operation = "PUT"
    val exception = new IOException("Cache I/O error")
    testExceptionMapping(operation, exception, "cache", "I/O error")
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
    val clearError       = errors.toAppError("CLEAR", timeoutException)

    getError match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains("GET"))
      case _                               => fail("Expected UpstreamUnavailable")
    }
    putError match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains("PUT"))
      case _                               => fail("Expected UpstreamUnavailable")
    }
    clearError match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains("CLEAR"))
      case _                               => fail("Expected UpstreamUnavailable")
    }
  }

  test("toAppError should preserve exception message") {
    val operation = "GET"
    val exception = new TimeoutException("Custom timeout message")
    val error     = errors.toAppError(operation, exception)

    error match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains("Custom timeout message"))
      case _                               => fail("Expected UpstreamUnavailable")
    }
  }

  test("toAppError should handle different operation types") {
    val operations = List("GET", "PUT", "DELETE", "CLEAR", "EXISTS", "SIZE")
    val exception  = new TimeoutException("Test timeout")

    operations.foreach { operation =>
      val error = errors.toAppError(operation, exception)
      error match {
        case e: AppError.UpstreamUnavailable =>
          assert(e.message.contains(operation))
          assert(e.isInstanceOf[AppError.UpstreamUnavailable])
        case _ => fail("Expected UpstreamUnavailable")
      }
    }
  }

  test("toAppError should handle empty operation name") {
    val exception = new TimeoutException("Test timeout")
    val error     = errors.toAppError("", exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    error match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains("Timeout"))
      case _                               => fail("Expected UpstreamUnavailable")
    }
  }

  test("toAppError should handle null exception message") {
    val operation = "GET"
    val exception = new TimeoutException(null)
    val error     = errors.toAppError(operation, exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    error match {
      case e: AppError.UpstreamUnavailable =>
        assert(e.message.contains("GET"))
        assert(e.message.contains("Timeout"))
      case _ => fail("Expected UpstreamUnavailable")
    }
  }

  test("error transformation should be consistent across different exception types") {
    val operations = List("GET", "PUT", "DELETE")
    val exceptions = List(
      new TimeoutException("Test timeout"),
      new IOException("Test IO error"),
      new RuntimeException("Test runtime error")
    )

    operations.foreach { operation =>
      exceptions.foreach { exception =>
        val error = errors.toAppError(operation, exception)

        exception match {
          case _: TimeoutException =>
            assert(error.isInstanceOf[AppError.UpstreamUnavailable])
            val upstreamError = error.asInstanceOf[AppError.UpstreamUnavailable]
            assertEquals(upstreamError.service, "cache")
            assert(upstreamError.message.contains("Timeout"))
            assert(upstreamError.message.contains(operation))

          case _: IOException =>
            assert(error.isInstanceOf[AppError.UpstreamUnavailable])
            val upstreamError = error.asInstanceOf[AppError.UpstreamUnavailable]
            assertEquals(upstreamError.service, "cache")
            assert(upstreamError.message.contains("I/O error"))
            assert(upstreamError.message.contains(operation))

          case _: RuntimeException =>
            assert(error.isInstanceOf[AppError.UnexpectedError])
            error match {
              case e: AppError.UnexpectedError => assertEquals(e.message, "Unexpected cache error")
              case _                           => fail("Expected UnexpectedError")
            }
        }
      }
    }
  }

  test("toAppError should handle very long operation names") {
    val longOperation = "VERY_LONG_OPERATION_NAME_THAT_MIGHT_CAUSE_ISSUES"
    val exception     = new TimeoutException("Test timeout")
    val error         = errors.toAppError(longOperation, exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    error match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains(longOperation))
      case _                               => fail("Expected UpstreamUnavailable")
    }
  }

  test("toAppError should handle special characters in operation names") {
    val specialOperation = "GET-WITH-SPECIAL-CHARS_123"
    val exception        = new TimeoutException("Test timeout")
    val error            = errors.toAppError(specialOperation, exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    error match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains(specialOperation))
      case _                               => fail("Expected UpstreamUnavailable")
    }
  }

  test("toAppError should handle null operation name") {
    val exception = new TimeoutException("Test timeout")
    val error     = errors.toAppError(null, exception)

    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    error match {
      case e: AppError.UpstreamUnavailable => assert(e.message.contains("Timeout"))
      case _                               => fail("Expected UpstreamUnavailable")
    }
  }
}
