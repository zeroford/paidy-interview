package forex.domain.error

sealed trait AppError extends Error
object AppError {
  final case class BadRequest(message: String) extends AppError
  final case class Validation(message: String) extends AppError
  final case class CalculationFailed(message: String) extends AppError
  final case class NotFound(message: String) extends AppError

  // upstream / infra
  final case class UpstreamAuthFailed(service: String, message: String) extends AppError
  final case class UpstreamUnavailable(service: String, message: String) extends AppError
  final case class RateLimited(service: String, message: String) extends AppError
  final case class DecodingFailed(service: String, message: String) extends AppError

  // generic
  final case class UnexpectedError(message: String) extends AppError
}
