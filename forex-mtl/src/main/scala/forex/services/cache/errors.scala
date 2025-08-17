package forex.services.cache

import java.io.IOException
import java.util.concurrent.TimeoutException

import forex.domain.error.AppError

object errors {

  def toAppError(op: String, t: Throwable): AppError = t match {
    case _: TimeoutException => AppError.UpstreamUnavailable("CacheService", s"$op -Timeout: ${t.getMessage}")
    case _: IOException      => AppError.UpstreamUnavailable("CacheService", s"$op -I/O error: ${t.getMessage}")
    case _                   => AppError.UnexpectedError("Unexpected cache error")
  }
}
