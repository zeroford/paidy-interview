package forex.services.rates

import cats.effect.IO
import munit.CatsEffectSuite

import forex.domain.cache.FetchStrategy
import forex.services.rates.concurrent.{ BucketLocks, RatesBucket }

class BucketLocksSpec extends CatsEffectSuite {

  test("BucketLocks should create successfully") {
    BucketLocks.create[IO].map { locks =>
      assert(locks != null)
    }
  }

  test("BucketLocks should execute withBucket for different buckets") {
    BucketLocks.create[IO].flatMap { locks =>
      locks
        .withBucket(RatesBucket.MostUsed) {
          IO.pure("success")
        }
        .map { result =>
          assertEquals(result, "success")
        }
    }
  }

  test("BucketLocks should execute withBuckets for all buckets") {
    BucketLocks.create[IO].flatMap { locks =>
      locks
        .withBuckets {
          IO.pure("success")
        }
        .map { result =>
          assertEquals(result, "success")
        }
    }
  }

  test("BucketLocks should handle exceptions in withBucket") {
    BucketLocks.create[IO].flatMap { locks =>
      val exception = new RuntimeException("test exception")
      locks
        .withBucket(RatesBucket.MostUsed) {
          IO.raiseError(exception)
        }
        .attempt
        .map { result =>
          assert(result.isLeft)
          assertEquals(result.left.toOption.get, exception)
        }
    }
  }

  test("BucketLocks.bucketFor should return correct bucket for each strategy") {
    assertEquals(BucketLocks.bucketFor(FetchStrategy.MostUsed), RatesBucket.MostUsed)
    assertEquals(BucketLocks.bucketFor(FetchStrategy.LeastUsed), RatesBucket.LeastUsed)
    assertEquals(BucketLocks.bucketFor(FetchStrategy.All), RatesBucket.LeastUsed)
  }

  test("RatesBucket should be sealed trait with case objects") {
    assert(RatesBucket.MostUsed.isInstanceOf[RatesBucket])
    assert(RatesBucket.LeastUsed.isInstanceOf[RatesBucket])
  }
}
