package forex.services.cache

import forex.domain.error.AppError

trait Algebra[F[_]] {
  def get[K, V](key: K): F[AppError Either Option[V]]
  def put[K, V](key: K, value: V): F[AppError Either Unit]
  def clear(): F[AppError Either Unit]
}
