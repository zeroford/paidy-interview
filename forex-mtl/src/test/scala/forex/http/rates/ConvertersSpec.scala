package forex.http.rates

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import java.time.OffsetDateTime
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ConvertersSpec extends AnyFunSuite with Matchers {
  import Converters._

  test("Rate.asGetApiResponse maps all fields correctly") {
    val rate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(BigDecimal(150.5)),
      timestamp = Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z"))
    )

    val res = rate.asGetApiResponse

    res.from shouldBe Currency.USD
    res.to shouldBe Currency.JPY
    res.price shouldBe Price(BigDecimal(150.5))
    res.timestamp shouldBe Timestamp(OffsetDateTime.parse("2024-08-04T12:34:56Z"))
  }
}
