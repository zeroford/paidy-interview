package forex.services.cache

import cats.effect.Sync
import cats.syntax.all._
import com.github.benmanes.caffeine.cache.Caffeine
import errors.CacheServiceError
import scala.concurrent.duration.FiniteDuration

class Service[F[_]: Sync](maxSize: Long, ttl: FiniteDuration) extends Algebra[F] {
  
  private val caffeine = Caffeine.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(java.time.Duration.ofSeconds(ttl.toSeconds))
    .build[String, Any]()
  
  def get[K, V](key: K): F[Option[V]] = 
    Sync[F].delay {
      val value = caffeine.getIfPresent(key.toString)
      if (value == null) None else Some(value.asInstanceOf[V])
    }.handleErrorWith { error =>
      Sync[F].raiseError(CacheServiceError.CacheOperationFailed(s"Failed to get cache key: ${error.getMessage}"))
    }
  
  def put[K, V](key: K, value: V): F[Unit] = 
    Sync[F].delay(caffeine.put(key.toString, value))
      .handleErrorWith { error =>
        Sync[F].raiseError(CacheServiceError.CacheOperationFailed(s"Failed to put cache key: ${error.getMessage}"))
      }
  
  def clear(): F[Unit] = 
    Sync[F].delay(caffeine.invalidateAll())
      .handleErrorWith { error =>
        Sync[F].raiseError(CacheServiceError.CacheOperationFailed(s"Failed to clear cache: ${error.getMessage}"))
      }
}

object Service {
  def apply[F[_]: Sync](maxSize: Long, ttl: FiniteDuration): Algebra[F] = new Service[F](maxSize, ttl)
}
