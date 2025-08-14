package forex.services.cache

import forex.domain.error.AppError

import java.io.IOException
import java.util.concurrent.TimeoutException

object errors {

  def toAppError(op: String, t: Throwable): AppError = t match {
    case _: TimeoutException => AppError.UpstreamUnavailable("CacheService", s"$op -Timeout: ${t.getMessage}")
    case _: IOException      => AppError.UpstreamUnavailable("CacheService", s"$op -I/O error: ${t.getMessage}")
    case _                   => AppError.UnexpectedError("Unexpected cache error")
  }
}
