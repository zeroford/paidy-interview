package forex.http.rates

import cats.implicits.toBifunctorOps
import forex.domain.currency.Currency
import forex.http.util.CurrencyErrorMappings
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher

object QueryParams {
  implicit val currencyQueryParamDecoder: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { str =>
      Currency.fromString(str).leftMap(CurrencyErrorMappings.toParseFailure)
    }

  object FromQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam   extends OptionalValidatingQueryParamDecoderMatcher[Currency]("to")
}