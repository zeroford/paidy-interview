package forex.services.cache

trait Algebra[F[_]] {
  def get[K, V](key: K): F[Option[V]]
  def put[K, V](key: K, value: V): F[Unit]
  def clear(): F[Unit]
}
