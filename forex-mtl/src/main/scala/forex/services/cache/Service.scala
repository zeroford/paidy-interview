package forex.services.cache

import scala.concurrent.duration.FiniteDuration
import cats.effect.Sync
import cats.syntax.all._
import com.github.blemale.scaffeine.Scaffeine
import forex.domain.error.AppError
import forex.services.cache.{ errors => Error }
import org.typelevel.log4cats.Logger

final class Service[F[_]: Sync: Logger](maxSize: Long, ttl: FiniteDuration) extends Algebra[F] {

  private val cache = Scaffeine()
    .maximumSize(maxSize)
    .expireAfterWrite(ttl)
    .build[String, Any]()

  override def get[K, V](key: K): F[AppError Either Option[V]] =
    Sync[F].delay(cache.getIfPresent(key.toString).asInstanceOf[Option[V]]).attempt.flatMap {
      case Right(Some(value)) =>
        Logger[F].debug(s"[Cache] get HIT, key:$key") >> value.some.asRight[AppError].pure[F]
      case Right(None) =>
        Logger[F].debug(s"[Cache] get MISS, key:$key") >> Option.empty[V].asRight[AppError].pure[F]
      case Left(e: Throwable) =>
        Logger[F].error(s"[Cache] get error, ${e.getMessage}") >> Error.toAppError("GET", e).asLeft[Option[V]].pure[F]
    }

  override def put[K, V](key: K, value: V): F[AppError Either Unit] =
    Sync[F].delay(cache.put(key.toString, value)).attempt.flatMap {
      case Right(_) =>
        Logger[F].debug(s"[Cache] put OK, key:$key") >> ().asRight[AppError].pure[F]
      case Left(e: Throwable) =>
        Logger[F]
          .error(s"[Cache] put error, key:$key, ${e.getMessage}") >> Error.toAppError("PUT", e).asLeft[Unit].pure[F]
    }

  override def clear(): F[AppError Either Unit] =
    Sync[F].delay(cache.invalidateAll()).attempt.flatMap {
      case Right(_) =>
        Logger[F].debug(s"[Cache] clear OK") >> ().asRight[AppError].pure[F]
      case Left(e: Throwable) =>
        Logger[F].error(s"[Cache] clear error, ${e.getMessage}") >> Error.toAppError("CLEAR", e).asLeft[Unit].pure[F]
    }

}

object Service {
  def apply[F[_]: Sync: Logger](maxSize: Long, ttl: FiniteDuration): Algebra[F] = new Service[F](maxSize, ttl)
}
