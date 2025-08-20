package forex.http.rates

import cats.syntax.bifunctor._
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher

import forex.domain.currency.Currency
import forex.domain.currency.errors.CurrencyError

object QueryParams {
  implicit val currencyQueryParamDecoder: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { str =>
      Currency.fromString(str).leftMap(CurrencyError.toParseFailure)
    }

  object FromQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("to")
}
