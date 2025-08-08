package forex.services.cache

object errors {

  sealed trait CacheServiceError extends Error
  object CacheServiceError {
    final case class CacheOperationFailed(message: String) extends CacheServiceError
    final case class MemoryPressure(message: String) extends CacheServiceError
    final case class InternalError(throwable: Throwable) extends CacheServiceError
  }
} 