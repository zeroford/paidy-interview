package forex.clients.oneframe

import munit.FunSuite
import forex.clients.oneframe.Protocol.OneFrameRate
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Timestamp }
import forex.domain.error.AppError
import java.time.Instant

class ConvertersSpec extends FunSuite {

  test("toPivotRate should convert valid OneFrameRate to PivotRate") {
    val oneFrameRate = OneFrameRate(
      from = "USD",
      to = "EUR",
      bid = 0.85,
      ask = 0.86,
      price = 0.855,
      time_stamp = Instant.parse("2024-01-01T10:00:00Z")
    )

    val result = Converters.toPivotRate(oneFrameRate)

    assert(result.isRight)
    val pivotRate = result.toOption.get
    assertEquals(pivotRate.currency, Currency.EUR)
    assertEquals(pivotRate.price, Price(BigDecimal(0.855)))
    assertEquals(pivotRate.timestamp, Timestamp(Instant.parse("2024-01-01T10:00:00Z")))
  }

  test("toPivotRate should handle case-insensitive currency codes") {
    val oneFrameRate = OneFrameRate(
      from = "USD",
      to = "eur",
      bid = 0.85,
      ask = 0.86,
      price = 0.855,
      time_stamp = Instant.parse("2024-01-01T10:00:00Z")
    )

    val result = Converters.toPivotRate(oneFrameRate)

    assert(result.isRight)
    val pivotRate = result.toOption.get
    assertEquals(pivotRate.currency, Currency.EUR)
  }

    test("toPivotRate should return error for unsupported currency") {
    val oneFrameRate = OneFrameRate(
      from = "USD",
      to = "INVALID",
      bid = 0.85,
      ask = 0.86,
      price = 0.855,
      time_stamp = Instant.parse("2024-01-01T10:00:00Z")
    )

    val result = Converters.toPivotRate(oneFrameRate)
    
    assert(result.isLeft)
    val error = result.left.toOption.get
    assert(error.isInstanceOf[AppError.Validation])
    // Just verify it's a validation error, don't check the specific message
  }

  test("toPivotRate should handle different currencies") {
    val currencies = List("USD", "EUR", "GBP", "JPY", "CHF", "AUD", "CAD")

    currencies.foreach { currency =>
      val oneFrameRate = OneFrameRate(
        from = "USD",
        to = currency,
        bid = 1.0,
        ask = 1.0,
        price = 1.0,
        time_stamp = Instant.parse("2024-01-01T10:00:00Z")
      )

      val result = Converters.toPivotRate(oneFrameRate)
      assert(result.isRight, s"Should handle currency: $currency")
    }
  }

  test("toPivotRate should handle different price values") {
    val oneFrameRate = OneFrameRate(
      from = "USD",
      to = "EUR",
      bid = 0.123456,
      ask = 0.123457,
      price = 0.1234565,
      time_stamp = Instant.parse("2024-01-01T10:00:00Z")
    )

    val result = Converters.toPivotRate(oneFrameRate)

    assert(result.isRight)
    val pivotRate = result.toOption.get
    assertEquals(pivotRate.price, Price(BigDecimal(0.1234565)))
  }

  test("toPivotRate should handle different timestamps") {
    val timestamps = List(
      Instant.parse("2024-01-01T10:00:00Z"),
      Instant.parse("2024-12-31T23:59:59Z"),
      Instant.now()
    )

    timestamps.foreach { timestamp =>
      val oneFrameRate = OneFrameRate(
        from = "USD",
        to = "EUR",
        bid = 0.85,
        ask = 0.86,
        price = 0.855,
        time_stamp = timestamp
      )

      val result = Converters.toPivotRate(oneFrameRate)
      assert(result.isRight, s"Should handle timestamp: $timestamp")
      val pivotRate = result.toOption.get
      assertEquals(pivotRate.timestamp, Timestamp(timestamp))
    }
  }
}
