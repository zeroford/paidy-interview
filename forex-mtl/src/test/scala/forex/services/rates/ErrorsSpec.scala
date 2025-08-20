package forex.services.rates

import forex.domain.error.AppError
import forex.domain.rates.{ PivotRate, Rate }
import forex.services.rates.errors
import munit.FunSuite

class ErrorsSpec extends FunSuite {

  test("notFound should create NotFound error with correct message") {
    val pair  = Rate.Pair(forex.domain.currency.Currency.USD, forex.domain.currency.Currency.EUR)
    val error = errors.notFound(pair)

    assert(error.isInstanceOf[AppError.NotFound])
    error match {
      case AppError.NotFound(message) =>
        assert(message.contains("USD"))
        assert(message.contains("EUR"))
        assert(message.contains("Could not extract rates"))
      case _ => fail("Expected NotFound error")
    }
  }

  test("notFound should handle different currency pairs") {
    val pair1 = Rate.Pair(forex.domain.currency.Currency.GBP, forex.domain.currency.Currency.JPY)
    val pair2 = Rate.Pair(forex.domain.currency.Currency.EUR, forex.domain.currency.Currency.GBP)

    val error1 = errors.notFound(pair1)
    val error2 = errors.notFound(pair2)

    assert(error1.isInstanceOf[AppError.NotFound])
    assert(error2.isInstanceOf[AppError.NotFound])
    error1 match {
      case AppError.NotFound(message) =>
        assert(message.contains("GBP"))
        assert(message.contains("JPY"))
      case _ => fail("Expected NotFound error")
    }
    error2 match {
      case AppError.NotFound(message) =>
        assert(message.contains("EUR"))
        assert(message.contains("GBP"))
      case _ => fail("Expected NotFound error")
    }
  }

  test("toAppError should create DecodingFailed error") {
    val service = "test-service"
    val message = "test message"
    val error   = errors.toAppError(service, message)

    assert(error.isInstanceOf[AppError.DecodingFailed])
    error match {
      case AppError.DecodingFailed(svc, msg) =>
        assert(svc == service)
        assert(msg == message)
      case _ => fail("Expected DecodingFailed error")
    }
  }

  test("toAppError should handle empty strings") {
    val error1 = errors.toAppError("", "test message")
    val error2 = errors.toAppError("test-service", "")

    assert(error1.isInstanceOf[AppError.DecodingFailed])
    assert(error2.isInstanceOf[AppError.DecodingFailed])
  }

  test("toAppError should handle special characters") {
    val service = "test@service#123"
    val message = "error with special chars: @#$%^&*()"
    val error   = errors.toAppError(service, message)

    assert(error.isInstanceOf[AppError.DecodingFailed])
    error match {
      case AppError.DecodingFailed(svc, msg) =>
        assert(svc == service)
        assert(msg == message)
      case _ => fail("Expected DecodingFailed error")
    }
  }

  test("combine should return base error when only base fails") {
    val baseError    = Left(AppError.NotFound("base not found"))
    val quoteSuccess = Right(
      Some(
        PivotRate(
          forex.domain.currency.Currency.USD,
          forex.domain.rates.Price(BigDecimal(1.0)),
          forex.domain.rates.Timestamp(java.time.Instant.now())
        )
      )
    )

    val result = errors.combine(baseError, quoteSuccess)

    assert(result.isInstanceOf[AppError.NotFound])
    result match {
      case AppError.NotFound(message) =>
        assert(message.contains("base not found"))
      case _ => fail("Expected NotFound error")
    }
  }

  test("combine should return quote error when only quote fails") {
    val baseSuccess = Right(
      Some(
        PivotRate(
          forex.domain.currency.Currency.USD,
          forex.domain.rates.Price(BigDecimal(1.0)),
          forex.domain.rates.Timestamp(java.time.Instant.now())
        )
      )
    )
    val quoteError = Left(AppError.Validation("quote validation failed"))

    val result = errors.combine(baseSuccess, quoteError)

    assert(result.isInstanceOf[AppError.Validation])
    result match {
      case AppError.Validation(message) =>
        assert(message.contains("quote validation failed"))
      case _ => fail("Expected Validation error")
    }
  }

  test("combine should return combined UpstreamUnavailable when both are UpstreamUnavailable") {
    val baseError  = Left(AppError.UpstreamUnavailable("CacheService", "base upstream error"))
    val quoteError = Left(AppError.UpstreamUnavailable("CacheService", "quote upstream error"))

    val result = errors.combine(baseError, quoteError)

    assert(result.isInstanceOf[AppError.UpstreamUnavailable])
    result match {
      case AppError.UpstreamUnavailable(_, message) =>
        assert(message.contains("base upstream error"))
        assert(message.contains("quote upstream error"))
      case _ => fail("Expected UpstreamUnavailable error")
    }
  }

  test("combine should return UnexpectedError for other error combinations") {
    val baseError  = Left(AppError.NotFound("base not found"))
    val quoteError = Left(AppError.Validation("quote validation failed"))

    val result = errors.combine(baseError, quoteError)

    assert(result.isInstanceOf[AppError.UnexpectedError])
    result match {
      case AppError.UnexpectedError(message) =>
        assert(message.contains("Unexpected cache error"))
      case _ => fail("Expected UnexpectedError")
    }
  }

  test("combine should handle None values in success cases") {
    val baseNone   = Right(None)
    val quoteError = Left(AppError.NotFound("quote not found"))

    val result = errors.combine(baseNone, quoteError)

    assert(result.isInstanceOf[AppError.NotFound])
    result match {
      case AppError.NotFound(message) =>
        assert(message.contains("quote not found"))
      case _ => fail("Expected NotFound error")
    }
  }

  test("combine should handle both None values") {
    val baseNone  = Right(None)
    val quoteNone = Right(None)

    val result = errors.combine(baseNone, quoteNone)

    assert(result.isInstanceOf[AppError.UnexpectedError])
    result match {
      case AppError.UnexpectedError(message) =>
        assert(message.contains("Unexpected cache error"))
      case _ => fail("Expected UnexpectedError")
    }
  }

  test("combine should handle different error types") {
    val baseError  = Left(AppError.CalculationFailed("calculation failed"))
    val quoteError = Left(AppError.Validation("rate validation failed"))

    val result = errors.combine(baseError, quoteError)

    assert(result.isInstanceOf[AppError.UnexpectedError])
    result match {
      case AppError.UnexpectedError(message) =>
        assert(message.contains("Unexpected cache error"))
      case _ => fail("Expected UnexpectedError")
    }
  }

}
