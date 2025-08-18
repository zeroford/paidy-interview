package forex.domain.rates

import cats.syntax.either._
import io.circe.{ Decoder, Encoder }

final case class Price(value: BigDecimal) extends AnyVal
object Price {
  private def fromBigDecimal(n: BigDecimal): Either[String, Price] =
    if (n.signum < 0) Left("Price must be non-negative")
    else Price(n).asRight

  def fromInt(n: Int): Either[String, Price] = fromBigDecimal(BigDecimal(n))

  implicit val encoder: Encoder[Price] = Encoder.encodeBigDecimal.contramap(_.value)
  implicit val decoder: Decoder[Price] = Decoder.decodeBigDecimal.emap(fromBigDecimal)
}
