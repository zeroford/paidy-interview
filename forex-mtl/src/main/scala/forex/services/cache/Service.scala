package forex.services.cache

import cats.effect.Sync
import cats.syntax.all._
import com.github.blemale.scaffeine.Scaffeine
import errors.CacheServiceError
import scala.concurrent.duration.FiniteDuration

class Service[F[_]: Sync](maxSize: Long, ttl: FiniteDuration) extends Algebra[F] {

  private val scaffeine = Scaffeine()
    .maximumSize(maxSize)
    .expireAfterWrite(ttl)
    .build[String, Any]()

  def get[K, V](key: K): F[Option[V]] =
    Sync[F]
      .delay(scaffeine.getIfPresent(key.toString).asInstanceOf[Option[V]])
      .handleErrorWith { error =>
        Sync[F].raiseError(CacheServiceError.CacheOperationFailed(
          s"Failed to get cache key: ${error.getMessage}"
        ))
      }

  def put[K, V](key: K, value: V): F[Unit] =
    Sync[F]
      .delay(scaffeine.put(key.toString, value))
      .handleErrorWith { error =>
        Sync[F].raiseError(CacheServiceError.CacheOperationFailed(s"Failed to put cache key: ${error.getMessage}"))
      }

  def clear(): F[Unit] =
    Sync[F]
      .delay(scaffeine.invalidateAll())
      .handleErrorWith { error =>
        Sync[F].raiseError(CacheServiceError.CacheOperationFailed(s"Failed to clear cache: ${error.getMessage}"))
      }
}

object Service {
  def apply[F[_]: Sync](maxSize: Long, ttl: FiniteDuration): Algebra[F] = new Service[F](maxSize, ttl)
}
