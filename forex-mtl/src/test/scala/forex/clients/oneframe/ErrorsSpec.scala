package forex.clients.oneframe

import forex.domain.error.AppError
import munit.FunSuite
import java.io.IOException
import java.net.{ ConnectException, SocketTimeoutException }
import java.util.concurrent.TimeoutException

class ErrorsSpec extends FunSuite {

  object TestData {
    def arbitraryTimeoutException: SocketTimeoutException =
      new SocketTimeoutException("Test timeout")

    def arbitraryConnectException: ConnectException =
      new ConnectException("Test connection")

    def arbitraryIOException: IOException =
      new IOException("Test IO")
  }

  def testExceptionMapping(exception: Throwable, expectedService: String, expectedMessageContains: String): Unit = {
    val error = errors.toAppError(exception)
    assert(error.isInstanceOf[AppError.UpstreamUnavailable])
    val upstreamError = error.asInstanceOf[AppError.UpstreamUnavailable]
    assertEquals(upstreamError.service, expectedService)
    assert(upstreamError.message.contains(expectedMessageContains))
  }

  def testStringMapping(
      message: String,
      expectedErrorType: Class[_ <: AppError],
      expectedService: Option[String] = None,
      expectedMessage: Option[String] = None
  ): Unit = {
    val error = errors.toAppError(message)
    assert(error.getClass == expectedErrorType)
    expectedService.foreach(service =>
      error match {
        case e: AppError.UpstreamAuthFailed => assertEquals(e.service, service)
        case e: AppError.RateLimited        => assertEquals(e.service, service)
        case _                              =>
      }
    )
    expectedMessage.foreach(msg =>
      error match {
        case e: AppError.Validation         => assertEquals(e.message, msg)
        case e: AppError.NotFound           => assertEquals(e.message, msg)
        case e: AppError.UpstreamAuthFailed => assertEquals(e.message, msg)
        case e: AppError.RateLimited        => assertEquals(e.message, msg)
        case e: AppError.UnexpectedError    => assertEquals(e.message, msg)
        case _                              => fail(s"Unexpected error type: ${error.getClass}")
      }
    )
  }

  def testStatusMapping(
      status: Int,
      expectedErrorType: Class[_ <: AppError],
      expectedService: Option[String] = None,
      expectedMessageContains: Option[String] = None
  ): Unit = {
    val error = errors.toAppError(status)
    assert(error.getClass == expectedErrorType)
    expectedService.foreach(service =>
      error match {
        case e: AppError.UpstreamAuthFailed  => assertEquals(e.service, service)
        case e: AppError.UpstreamUnavailable => assertEquals(e.service, service)
        case e: AppError.RateLimited         => assertEquals(e.service, service)
        case _                               =>
      }
    )
    expectedMessageContains.foreach(msg =>
      error match {
        case e: AppError.BadRequest          => assert(e.message.contains(msg))
        case e: AppError.NotFound            => assert(e.message.contains(msg))
        case e: AppError.UpstreamAuthFailed  => assert(e.message.contains(msg))
        case e: AppError.RateLimited         => assert(e.message.contains(msg))
        case e: AppError.UpstreamUnavailable => assert(e.message.contains(msg))
        case e: AppError.UnexpectedError     => assert(e.message.contains(msg))
        case _                               => fail(s"Unexpected error type: ${error.getClass}")
      }
    )
  }

  test("toAppError with SocketTimeoutException should return UpstreamUnavailable") {
    val exception = new SocketTimeoutException("Connection timeout")
    testExceptionMapping(exception, "one-frame", "Timeout")
  }

  test("toAppError with TimeoutException should return UpstreamUnavailable") {
    val exception = new TimeoutException("Request timeout")
    testExceptionMapping(exception, "one-frame", "Timeout")
  }

  test("toAppError with ConnectException should return UpstreamUnavailable") {
    val exception = new ConnectException("Connection refused")
    testExceptionMapping(exception, "one-frame", "Unavailable")
  }

  test("toAppError with IOException should return UpstreamUnavailable") {
    val exception = new IOException("Network error")
    testExceptionMapping(exception, "one-frame", "Unavailable")
  }

  test("toAppError with unknown exception should return UnexpectedError") {
    val exception = new RuntimeException("Unknown error")
    val error     = errors.toAppError(exception)

    assert(error.isInstanceOf[AppError.UnexpectedError])
    val unexpectedError = error.asInstanceOf[AppError.UnexpectedError]
    assert(unexpectedError.message.contains("Unexpected upstream error"))
  }

  test("toAppError with 'Invalid Currency Pair' should return Validation") {
    testStringMapping(
      "Invalid Currency Pair",
      classOf[AppError.Validation],
      expectedMessage = Some("Invalid currency pair")
    )
  }

  test("toAppError with 'No currency pair provided' should return Validation") {
    testStringMapping(
      "No currency pair provided",
      classOf[AppError.Validation],
      expectedMessage = Some("Invalid currency pair")
    )
  }

  test("toAppError with 'Quota reached' should return RateLimited") {
    testStringMapping(
      "Quota reached",
      classOf[AppError.RateLimited],
      expectedService = Some("one-frame"),
      expectedMessage = Some("Rate limited")
    )
  }

  test("toAppError with 'Rate limited' should return RateLimited") {
    testStringMapping(
      "Rate limited",
      classOf[AppError.RateLimited],
      expectedService = Some("one-frame"),
      expectedMessage = Some("Rate limited")
    )
  }

  test("toAppError with 'Forbidden' should return UpstreamAuthFailed") {
    testStringMapping(
      "Forbidden",
      classOf[AppError.UpstreamAuthFailed],
      expectedService = Some("one-frame"),
      expectedMessage = Some("Upstream service authentication failed")
    )
  }

  test("toAppError with 'Empty Rate' should return NotFound") {
    testStringMapping("Empty Rate", classOf[AppError.NotFound], expectedMessage = Some("No rate found"))
  }

  test("toAppError with 'No Rate Found' should return NotFound") {
    testStringMapping("No Rate Found", classOf[AppError.NotFound], expectedMessage = Some("No rate found"))
  }

  test("toAppError with unknown string should return UnexpectedError") {
    testStringMapping("Unknown error", classOf[AppError.UnexpectedError], expectedMessage = Some("Unexpected error"))
  }

  test("toAppError with status 400 should return BadRequest") {
    testStatusMapping(400, classOf[AppError.BadRequest], expectedMessageContains = Some("Bad request"))
  }

  test("toAppError with status 401 should return UpstreamAuthFailed") {
    testStatusMapping(
      401,
      classOf[AppError.UpstreamAuthFailed],
      expectedService = Some("one-frame"),
      expectedMessageContains = Some("Upstream service authentication failed")
    )
  }

  test("toAppError with status 403 should return UpstreamAuthFailed") {
    testStatusMapping(
      403,
      classOf[AppError.UpstreamAuthFailed],
      expectedService = Some("one-frame"),
      expectedMessageContains = Some("Upstream service authentication failed")
    )
  }

  test("toAppError with status 404 should return NotFound") {
    testStatusMapping(404, classOf[AppError.NotFound], expectedMessageContains = Some("No rate found"))
  }

  test("toAppError with status 429 should return RateLimited") {
    testStatusMapping(
      429,
      classOf[AppError.RateLimited],
      expectedService = Some("one-frame"),
      expectedMessageContains = Some("Rate limited")
    )
  }

  test("toAppError with status 500 should return UpstreamUnavailable") {
    testStatusMapping(
      500,
      classOf[AppError.UpstreamUnavailable],
      expectedService = Some("one-frame"),
      expectedMessageContains = Some("Upstream error 500")
    )
  }

  test("toAppError with status 503 should return UpstreamUnavailable") {
    testStatusMapping(
      503,
      classOf[AppError.UpstreamUnavailable],
      expectedService = Some("one-frame"),
      expectedMessageContains = Some("Upstream error 503")
    )
  }

  test("toAppError with unknown status should return UnexpectedError") {
    val error = errors.toAppError(399)
    println(s"Error type: ${error.getClass}, Error: $error")
    assert(error.isInstanceOf[AppError.UnexpectedError])
  }

  test("error transformation should be consistent across different input types") {
    val testCases = List(
      (new SocketTimeoutException("test"), "one-frame", "Timeout"),
      (new ConnectException("test"), "one-frame", "Unavailable"),
      (new IOException("test"), "one-frame", "Unavailable"),
      ("Invalid Currency Pair", "Invalid currency pair"),
      ("Quota reached", "Rate limited"),
      ("Forbidden", "Upstream service authentication failed"),
      ("Empty Rate", "No rate found"),
      (400, "Bad request"),
      (403, "Upstream service authentication failed"),
      (404, "No rate found"),
      (429, "Rate limited"),
      (500, "Upstream error 500")
    )

    testCases.foreach {
      case (input: Throwable, service: String, messageContains: String) =>
        val error = errors.toAppError(input)
        assert(error.isInstanceOf[AppError.UpstreamUnavailable])
        val upstreamError = error.asInstanceOf[AppError.UpstreamUnavailable]
        assertEquals(upstreamError.service, service)
        assert(upstreamError.message.contains(messageContains))

      case (input: String, expectedMessage: String) =>
        val error = errors.toAppError(input)
        error match {
          case e: AppError.Validation         => assertEquals(e.message, expectedMessage)
          case e: AppError.NotFound           => assertEquals(e.message, expectedMessage)
          case e: AppError.UpstreamAuthFailed => assertEquals(e.message, expectedMessage)
          case e: AppError.RateLimited        => assertEquals(e.message, expectedMessage)
          case e: AppError.UnexpectedError    => assertEquals(e.message, expectedMessage)
          case _                              => fail(s"Unexpected error type: ${error.getClass}")
        }

      case (input: Int, expectedMessageContains: String) =>
        val error = errors.toAppError(input)
        error match {
          case e: AppError.BadRequest          => assert(e.message.contains(expectedMessageContains))
          case e: AppError.NotFound            => assert(e.message.contains(expectedMessageContains))
          case e: AppError.UpstreamAuthFailed  => assert(e.message.contains(expectedMessageContains))
          case e: AppError.RateLimited         => assert(e.message.contains(expectedMessageContains))
          case e: AppError.UpstreamUnavailable => assert(e.message.contains(expectedMessageContains))
          case e: AppError.UnexpectedError     => assert(e.message.contains(expectedMessageContains))
          case _                               => fail(s"Unexpected error type: ${error.getClass}")
        }
    }
  }
}
