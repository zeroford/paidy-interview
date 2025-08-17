package forex.domain.rates

import io.circe.{ Decoder, Encoder }

final case class Price(value: BigDecimal) extends AnyVal
object Price {
  def fromInt(value: Integer): Price =
    Price(BigDecimal(value))

  implicit val encoder: Encoder[Price] = Encoder.encodeBigDecimal.contramap(_.value)
  implicit val decoder: Decoder[Price] = Decoder.decodeBigDecimal.map(Price.apply)
}
