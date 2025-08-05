package forex.http.rates

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import java.time.OffsetDateTime
import munit.FunSuite

class ConvertersSpec extends FunSuite {
  import Converters._

  test("Rate.asGetApiResponse maps all fields correctly") {
    val rate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal(150.5)),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z"))
    )

    val res = rate.asGetApiResponse

    assertEquals(res.from, Currency.USD)
    assertEquals(res.to, Currency.JPY)
    assertEquals(res.price, Price(BigDecimal(150.5)))
    assertEquals(res.timestamp, Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z")))
  }
}
