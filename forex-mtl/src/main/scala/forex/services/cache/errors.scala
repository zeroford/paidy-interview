package forex.services.cache

import forex.domain.error.AppError

import java.io.IOException
import java.util.concurrent.TimeoutException

object errors {

  def toAppError(op: String, t: Throwable): AppError = t match {
    case _: TimeoutException => AppError.UpstreamUnavailable("cache", s"$op -Timeout")
    case _: IOException      => AppError.UpstreamUnavailable("cache", s"$op -I/O error")
    case _                   => AppError.UnexpectedError("Unexpected cache error")
  }
}
