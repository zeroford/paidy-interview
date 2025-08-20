package forex.clients.oneframe

import munit.FunSuite
import forex.clients.oneframe.Protocol.OneFrameRate
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Timestamp }
import java.time.Instant

class ConvertersSpec extends FunSuite {

  test("toPivotRate converts valid OneFrameRate to PivotRate") {
    val r = OneFrameRate(
      "USD",
      "EUR",
      BigDecimal(0.85),
      BigDecimal(0.86),
      BigDecimal(0.855),
      Instant.parse("2024-01-01T10:00:00Z")
    )
    val res = Converters.toPivotRate(r)
    res match {
      case Right(p) =>
        assertEquals(p.currency, Currency.EUR)
        assertEquals(p.price, Price(BigDecimal(0.855)))
        assertEquals(p.timestamp, Timestamp(Instant.parse("2024-01-01T10:00:00Z")))
      case Left(e) => fail(s"unexpected error: $e")
    }
  }

  test("toPivotRate accepts multiple major currencies") {
    val ts     = Instant.parse("2024-01-01T10:00:00Z")
    val inputs = List(
      OneFrameRate("USD", "JPY", 0.0085, 0.0086, 0.00855, ts),
      OneFrameRate("EUR", "GBP", 0.85, 0.86, 0.855, ts),
      OneFrameRate("GBP", "CHF", 1.12, 1.13, 1.125, ts)
    )
    inputs.foreach { in =>
      Converters.toPivotRate(in) match {
        case Right(p) => assert(p.price.value > 0)
        case Left(e)  => fail(s"unexpected error: $e")
      }
    }
  }
}
