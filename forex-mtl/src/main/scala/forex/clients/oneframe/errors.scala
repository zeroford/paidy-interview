package forex.clients.oneframe

import forex.domain.error.AppError

import java.io.IOException
import java.net.{ ConnectException, SocketTimeoutException }
import java.util.concurrent.TimeoutException

object errors {

  def toAppError(t: Throwable): AppError = t match {
    case _: SocketTimeoutException | _: TimeoutException =>
      AppError.UpstreamUnavailable("one-frame", "Timeout")
    case _: ConnectException | _: IOException =>
      AppError.UpstreamUnavailable("one-frame", "Unavailable")
    case _ =>
      AppError.UnexpectedError("Unexpected upstream error")
  }

  def toAppError(error: String): AppError =
    error match {
      case "Invalid Currency Pair" | "No currency pair provided" => AppError.Validation("Invalid currency pair")
      case "Quota reached" | "Rate limited"                      => AppError.RateLimited("one-frame", "Rate limited")
      case "Forbidden" => AppError.UpstreamAuthFailed("one-frame", "Upstream service authentication failed")
      case "Empty Rate" | "No Rate Found" => AppError.NotFound("No rate found")
      case _                              => AppError.UnexpectedError("Unexpected error")
    }

  def toAppError(service: String, message: String): AppError = AppError.DecodingFailed(service, message)

  def toAppError(status: Int, body: String = ""): AppError =
    status match {
      case 400 =>
        AppError.BadRequest(s"Bad request: $body")
      case 401 | 403 =>
        AppError.UpstreamAuthFailed("one-frame", "Upstream service authentication failed")
      case 404 =>
        AppError.NotFound("No rate found")
      case 429 =>
        AppError.RateLimited("one-frame", "Rate limited")
      case x if x >= 500 =>
        AppError.UpstreamUnavailable("one-frame", s"Upstream error $x: $body")
      case _ =>
        AppError.UnexpectedError("Unexpected error")
    }

}
