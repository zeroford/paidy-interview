package forex.clients.oneframe

import java.io.IOException
import java.net.{ ConnectException, SocketTimeoutException }
import java.util.concurrent.TimeoutException

object errors {

  sealed trait OneFrameError extends Error
  object OneFrameError {
    final case class HttpError(code: Int, str: String) extends OneFrameError
    final case class LookupFailed(str: String) extends OneFrameError
    final case class DecodingError(str: String) extends OneFrameError
    final case class ConnectionError(str: String) extends OneFrameError
    final case class UnexpectedError(str: String) extends OneFrameError

    def nonEmpty: OneFrameError = LookupFailed("Pairs must be non-empty")
    def noRateFound: OneFrameError = LookupFailed("No rate found")
    def decodedFailed(str: String): OneFrameError = DecodingError(s"Failed to decoded $str")

    def fromThrowable(e: Throwable): OneFrameError = e match {
      case e: SocketTimeoutException => ConnectionError("OneFrameAPI: Timeout - " + e.getMessage)
      case e: TimeoutException       => ConnectionError("OneFrameAPI: Timeout - " + e.getMessage)
      case e: ConnectException       => ConnectionError("OneFrameAPI: Connection failed - " + e.getMessage)
      case e: IOException            => ConnectionError(e.getMessage)
      case _                         => UnexpectedError(e.getMessage)
    }

    def fromApiError(error: String): OneFrameError =
      error match {
        case "Invalid Currency Pair" | "No currency pair provided" =>
          LookupFailed("OneFrameAPI: Invalid currency pair")
        case "Quota reached" | "Rate limited" =>
          LookupFailed("OneFrameAPI: Rate limited")
        case "Forbidden" =>
          LookupFailed("OneFrameAPI: Invalid token")
        case other       =>
          UnexpectedError(s"Unexpected error from OneFrame: $other")
      }

  }
}
