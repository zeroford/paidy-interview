package forex.http.rates

import java.time.Instant

import munit.FunSuite

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.http.rates.Converters

class ConvertersSpec extends FunSuite {

  private val fixedInstant = Instant.parse("2024-08-04T12:34:56Z")
  private val testRate     = Rate(
    pair = Rate.Pair(Currency.USD, Currency.EUR),
    price = Price(BigDecimal(0.85)),
    timestamp = Timestamp(fixedInstant)
  )

  test("GetApiResponse should convert from Rate correctly") {
    val response = Converters.fromRate(testRate)

    assertEquals(response.from, Currency.USD)
    assertEquals(response.to, Currency.EUR)
    assertEquals(response.price, Price(BigDecimal(0.85)))
    assertEquals(response.timestamp, Timestamp(fixedInstant))
  }

  test("GetApiResponse should handle different currency pairs") {
    val rate2 = Rate(
      pair = Rate.Pair(Currency.EUR, Currency.GBP),
      price = Price(BigDecimal(0.90)),
      timestamp = Timestamp(fixedInstant)
    )
    val response = Converters.fromRate(rate2)

    assertEquals(response.from, Currency.EUR)
    assertEquals(response.to, Currency.GBP)
    assertEquals(response.price, Price(BigDecimal(0.90)))
  }

  test("GetApiResponse should handle different prices") {
    val rate3 = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal(123.45)),
      timestamp = Timestamp(fixedInstant)
    )
    val response = Converters.fromRate(rate3)

    assertEquals(response.from, Currency.USD)
    assertEquals(response.to, Currency.JPY)
    assertEquals(response.price, Price(BigDecimal(123.45)))
  }

  test("GetApiResponse should handle same currency pair") {
    val sameCurrencyRate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.USD),
      price = Price(BigDecimal(1.0)),
      timestamp = Timestamp(fixedInstant)
    )
    val response = Converters.fromRate(sameCurrencyRate)

    assertEquals(response.from, Currency.USD)
    assertEquals(response.to, Currency.USD)
    assertEquals(response.price, Price(BigDecimal(1.0)))
  }

  test("GetApiResponse should be immutable") {
    val response1 = Converters.fromRate(testRate)
    val response2 = response1.copy(from = Currency.EUR)

    assertEquals(response1.from, Currency.USD)
    assertEquals(response2.from, Currency.EUR)
  }

  test("fractionalPip should return scale 3 for JPY pairs") {
    val jpyFromRate = Rate(
      pair = Rate.Pair(Currency.JPY, Currency.USD),
      price = Price(BigDecimal(0.0085)),
      timestamp = Timestamp(fixedInstant)
    )
    val jpyFromResponse = Converters.fromRate(jpyFromRate)
    assertEquals(jpyFromResponse.price.value.scale, 3, "JPY->USD should have scale 3")

    val jpyToRate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal(123.456)),
      timestamp = Timestamp(fixedInstant)
    )
    val jpyToResponse = Converters.fromRate(jpyToRate)
    assertEquals(jpyToResponse.price.value.scale, 3, "USD->JPY should have scale 3")

    val jpyToJpyRate = Rate(
      pair = Rate.Pair(Currency.JPY, Currency.JPY),
      price = Price(BigDecimal(1.0)),
      timestamp = Timestamp(fixedInstant)
    )
    val jpyToJpyResponse = Converters.fromRate(jpyToJpyRate)
    assertEquals(jpyToJpyResponse.price.value.scale, 3, "JPY->JPY should have scale 3")
  }

  test("fractionalPip should return scale 5 for non-JPY pairs") {
    val usdEurRate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.EUR),
      price = Price(BigDecimal(0.85123)),
      timestamp = Timestamp(fixedInstant)
    )
    val usdEurResponse = Converters.fromRate(usdEurRate)
    assertEquals(usdEurResponse.price.value.scale, 5, "USD->EUR should have scale 5")

    val eurGbpRate = Rate(
      pair = Rate.Pair(Currency.EUR, Currency.GBP),
      price = Price(BigDecimal(0.90123)),
      timestamp = Timestamp(fixedInstant)
    )
    val eurGbpResponse = Converters.fromRate(eurGbpRate)
    assertEquals(eurGbpResponse.price.value.scale, 5, "EUR->GBP should have scale 5")

    val audCadRate = Rate(
      pair = Rate.Pair(Currency.AUD, Currency.CAD),
      price = Price(BigDecimal(0.91234)),
      timestamp = Timestamp(fixedInstant)
    )
    val audCadResponse = Converters.fromRate(audCadRate)
    assertEquals(audCadResponse.price.value.scale, 5, "AUD->CAD should have scale 5")
  }

  test("fractionalPip should round prices correctly with HALF_UP") {
    val jpyRateRoundingUp = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal(123.4567)),
      timestamp = Timestamp(fixedInstant)
    )
    val jpyResponseRoundingUp = Converters.fromRate(jpyRateRoundingUp)
    assertEquals(jpyResponseRoundingUp.price.value, BigDecimal(123.457), "JPY should round up to 3 decimal places")

    val jpyRateRoundingDown = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal(123.4564)),
      timestamp = Timestamp(fixedInstant)
    )
    val jpyResponseRoundingDown = Converters.fromRate(jpyRateRoundingDown)
    assertEquals(jpyResponseRoundingDown.price.value, BigDecimal(123.456), "JPY should round down to 3 decimal places")

    val eurRateRoundingUp = Rate(
      pair = Rate.Pair(Currency.USD, Currency.EUR),
      price = Price(BigDecimal(0.851234)),
      timestamp = Timestamp(fixedInstant)
    )
    val eurResponseRoundingUp = Converters.fromRate(eurRateRoundingUp)
    assertEquals(eurResponseRoundingUp.price.value, BigDecimal(0.85123), "EUR should round up to 5 decimal places")

    val eurRateRoundingDown = Rate(
      pair = Rate.Pair(Currency.USD, Currency.EUR),
      price = Price(BigDecimal(0.851234)),
      timestamp = Timestamp(fixedInstant)
    )
    val eurResponseRoundingDown = Converters.fromRate(eurRateRoundingDown)
    assertEquals(eurResponseRoundingDown.price.value, BigDecimal(0.85123), "EUR should round down to 5 decimal places")
  }

  test("fractionalPip should handle edge cases") {
    val smallRate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal(0.001)),
      timestamp = Timestamp(fixedInstant)
    )
    val smallResponse = Converters.fromRate(smallRate)
    assertEquals(smallResponse.price.value.scale, 3, "Small JPY rate should have scale 3")

    val largeRate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.EUR),
      price = Price(BigDecimal(999999.999999)),
      timestamp = Timestamp(fixedInstant)
    )
    val largeResponse = Converters.fromRate(largeRate)
    assertEquals(largeResponse.price.value.scale, 5, "Large EUR rate should have scale 5")

    val exactRate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal(100.000)),
      timestamp = Timestamp(fixedInstant)
    )
    val exactResponse = Converters.fromRate(exactRate)
    assertEquals(exactResponse.price.value, BigDecimal(100.000), "Exact JPY rate should maintain precision")
  }

  test("fractionalPip should work with all major currencies") {
    val majorCurrencies = List(Currency.EUR, Currency.GBP, Currency.CHF, Currency.CAD, Currency.AUD, Currency.NZD)

    majorCurrencies.foreach { currency =>
      val rate = Rate(
        pair = Rate.Pair(Currency.USD, currency),
        price = Price(BigDecimal(1.234567)),
        timestamp = Timestamp(fixedInstant)
      )
      val response = Converters.fromRate(rate)
      assertEquals(response.price.value.scale, 5, s"USD->$currency should have scale 5")
    }
  }
}
