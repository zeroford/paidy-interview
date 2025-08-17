package forex.http.rates

import java.time.Instant

import munit.FunSuite

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import forex.http.rates.Converters._

class ConvertersSpec extends FunSuite {

  private val fixedInstant = Instant.parse("2024-08-04T12:34:56Z")
  private val testRate     = Rate(
    pair = Rate.Pair(Currency.USD, Currency.EUR),
    price = Price(BigDecimal(0.85)),
    timestamp = Timestamp(fixedInstant)
  )

  test("GetApiResponse should convert from Rate correctly") {
    val response = testRate.asGetApiResponse

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
    val response = rate2.asGetApiResponse

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
    val response = rate3.asGetApiResponse

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
    val response = sameCurrencyRate.asGetApiResponse

    assertEquals(response.from, Currency.USD)
    assertEquals(response.to, Currency.USD)
    assertEquals(response.price, Price(BigDecimal(1.0)))
  }

  test("GetApiResponse should be immutable") {
    val response1 = testRate.asGetApiResponse
    val response2 = response1.copy(from = Currency.EUR)

    assertEquals(response1.from, Currency.USD)
    assertEquals(response2.from, Currency.EUR)
  }
}
