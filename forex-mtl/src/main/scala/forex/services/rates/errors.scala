package forex.services.rates

import forex.domain.error.AppError
import forex.domain.error.AppError.UpstreamUnavailable
import forex.domain.rates.{ PivotRate, Rate }

object errors {

  def notFound(pair: Rate.Pair): AppError =
    AppError.NotFound(s"Could not extract rates for ${pair.from} and ${pair.to}")

  def toAppError(service: String, message: String): AppError = AppError.DecodingFailed(service, message)

  def combine(
      baseError: Either[AppError, Option[PivotRate]],
      quoteError: Either[AppError, Option[PivotRate]]
  ): AppError =
    (baseError, quoteError) match {
      case (Left(base), Right(_))                                              => base
      case (Right(_), Left(quote))                                             => quote
      case (Left(base: UpstreamUnavailable), Left(quote: UpstreamUnavailable)) =>
        AppError.UpstreamUnavailable("CacheService", s"${base.message}, ${quote.message}")
      case (_, _) => AppError.UnexpectedError("Unexpected cache error")
    }
}
