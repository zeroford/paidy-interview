package forex.http.rates

import cats.syntax.bifunctor._
import forex.domain.currency.Currency
import forex.domain.currency.errors.CurrencyError
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher

object QueryParams {
  implicit val currencyQueryParamDecoder: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { str =>
      Currency.fromString(str).leftMap(CurrencyError.toParseFailure)
    }

  object FromQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("to")
}
