package forex.domain.currency

import cats.Show
import forex.domain.currency.CurrencyError

sealed trait Currency
object Currency {

  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  private val all: List[Currency] = List(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)

  val supported: List[String] = all.map(_.toString)

  def fromString(s: String): Either[CurrencyError, Currency] =
    if (s.trim.isEmpty) Left(CurrencyError.Empty)
    else {
      all.find(_.toString == s.toUpperCase) match {
        case Some(c) => Right(c)
        case None    => Left(CurrencyError.Unsupported(s))
      }
    }

  implicit val show: Show[Currency] = Show.show(_.toString)
}
