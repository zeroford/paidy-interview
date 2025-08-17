package forex.domain.rates

import io.circe.{ Decoder, Encoder }

import scala.math.BigDecimal.RoundingMode

final case class Price(value: BigDecimal) extends AnyVal
object Price {
  private val Scale = 10
  private val Mode  = RoundingMode.HALF_UP

  private def fromBigDecimal(n: BigDecimal): Either[String, Price] =
    if (n.signum < 0) Left("Price must be non-negative")
    else Right(new Price(n.setScale(Scale, Mode)))

  def fromInt(n: Int): Either[String, Price] = fromBigDecimal(BigDecimal(n))

  implicit val encoder: Encoder[Price] = Encoder.encodeBigDecimal.contramap(_.value)
  implicit val decoder: Decoder[Price] = Decoder.decodeBigDecimal.emap(fromBigDecimal)
}
