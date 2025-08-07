package forex.domain.currency

import forex.domain.currency.CurrencyError
import io.circe.{ Decoder, Encoder }

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

  implicit val encoder: Encoder[Currency] = Encoder.encodeString.contramap(_.toString)
  implicit val decoder: Decoder[Currency] = Decoder.decodeString.emap(fromString(_).left.map(_.toString))
}
