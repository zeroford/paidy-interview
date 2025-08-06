package forex.integrations.oneframe

import forex.integrations.oneframe.Protocol.OneFrameApiError

object errors {

  sealed trait OneFrameError extends Error
  object OneFrameError {
    final case class OneFrameLookupFailed(msg: String) extends OneFrameError
    final case class HttpError(status: Int, message: String) extends OneFrameError
    final case class DecodingError(message: String) extends OneFrameError
    final case class TimeoutError(message: String) extends OneFrameError
    final case class NetworkError(message: String) extends OneFrameError
    final case class InternalServerError(throwable: Throwable) extends OneFrameError
    final case class UnknownError(e: Throwable) extends OneFrameError

    def fromApiError(apiError: OneFrameApiError): OneFrameError =
      apiError.error match {
        case "Invalid Currency Pair" | "No currency pair provided" =>
          OneFrameError.OneFrameLookupFailed("Invalid currency pair")
        case "Quota reached" =>
          OneFrameError.OneFrameLookupFailed("Rate limited")
        case other =>
          OneFrameError.OneFrameLookupFailed("Unexpected Error from OneFrame: " + other)
      }

    def fromThrowable(e: Throwable): OneFrameError = e match {
      case e: java.net.SocketTimeoutException       => OneFrameError.TimeoutError(e.getMessage)
      case e: java.util.concurrent.TimeoutException => OneFrameError.TimeoutError(e.getMessage)
      case e: java.net.ConnectException             => OneFrameError.NetworkError(e.getMessage)
      case e: java.io.IOException                   => OneFrameError.NetworkError(e.getMessage)
      case e: java.net.UnknownHostException         => OneFrameError.NetworkError(e.getMessage)
      case _                                        => OneFrameError.UnknownError(e)
    }
  }
}
