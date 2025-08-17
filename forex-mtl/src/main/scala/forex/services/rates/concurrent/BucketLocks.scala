package forex.services.rates.concurrent

import cats.effect.Async
import cats.effect.kernel.Concurrent
import cats.effect.std.Semaphore
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.domain.cache.FetchStrategy

final class BucketLocks[F[_]: Concurrent](most: Semaphore[F], other: Semaphore[F]) {
  private def sem(b: RatesBucket) = b match {
    case RatesBucket.MostUsed  => most
    case RatesBucket.LeastUsed => other
  }

  def withBucket[A](b: RatesBucket)(fa: F[A]): F[A] = sem(b).permit.use(_ => fa)

  def withBuckets[A](fa: F[A]): F[A] = {
    val list: List[RatesBucket] = List(RatesBucket.MostUsed, RatesBucket.LeastUsed)
    list.foldRight(fa)((b, acc) => sem(b).permit.use(_ => acc))
  }
}

object BucketLocks {
  def create[F[_]: Async]: F[BucketLocks[F]] = for {
    most <- Semaphore.apply(1)
    other <- Semaphore.apply(1)
  } yield new BucketLocks[F](most, other)

  def bucketFor(strategy: FetchStrategy): RatesBucket =
    if (strategy == FetchStrategy.MostUsed) RatesBucket.MostUsed else RatesBucket.LeastUsed
}
