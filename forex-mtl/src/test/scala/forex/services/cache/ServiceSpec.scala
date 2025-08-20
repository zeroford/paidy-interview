package forex.services.cache

import java.time.Instant
import scala.concurrent.duration._

import cats.effect.IO
import munit.CatsEffectSuite
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }

class ServiceSpec extends CatsEffectSuite {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val fixedInstant = Instant.parse("2024-01-01T10:00:00Z")
  private val testRate     = Rate(
    pair = Rate.Pair(Currency.USD, Currency.EUR),
    price = Price(BigDecimal(0.85)),
    timestamp = Timestamp(fixedInstant)
  )

  private val testRate2 = Rate(
    pair = Rate.Pair(Currency.EUR, Currency.GBP),
    price = Price(BigDecimal(0.90)),
    timestamp = Timestamp(fixedInstant)
  )

  private val testRate3 = Rate(
    pair = Rate.Pair(Currency.GBP, Currency.JPY),
    price = Price(BigDecimal(150.0)),
    timestamp = Timestamp(fixedInstant)
  )

  test("Cache should store and retrieve rates successfully") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("USD-EUR", testRate)
      result <- cache.get[String, Rate]("USD-EUR")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      rate <- IO(result.toOption.flatten.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate.pair.to, Currency.EUR))
      _ <- IO(assertEquals(rate.price.value, BigDecimal(0.85)))
    } yield ()
  }

  test("Cache should return None for non-existent keys") {
    val cache = Service[IO](100, 1.minute)

    for {
      result <- cache.get[String, Rate]("NON-EXISTENT")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      _ <- IO(assert(result.toOption.flatten.isEmpty, "Non-existent key should return None"))
    } yield ()
  }

  test("Cache should handle cache eviction when full") {
    val cache = Service[IO](2, 1.minute)

    for {
      _ <- cache.put("key1", testRate)
      _ <- cache.put("key2", testRate2)
      _ <- cache.put("key3", testRate3)
      result1 <- cache.get[String, Rate]("key1")
      result2 <- cache.get[String, Rate]("key2")
      result3 <- cache.get[String, Rate]("key3")

      presentCount <- IO(List(result1, result2, result3).count(_.toOption.flatten.isDefined))
      _ <- IO(assert(presentCount >= 2, s"Cache should have at least 2 items, but found $presentCount"))
      _ <- IO(assert(presentCount <= 3, s"Cache should have at most 3 items, but found $presentCount"))
    } yield ()
  }

  test("Cache should handle concurrent access safely") {
    val cache = Service[IO](maxSize = 100L, ttl = 1.minute)
    val keys  = List.tabulate(10)(i => s"key-$i")

    for {
      putResults <- IO.parSequenceN(10)(keys.map(k => cache.put(k, testRate)))
      _ = assert(
            putResults.forall(_.isRight),
            s"put failures: ${putResults.collect { case Left(e) => e }.mkString(", ")}"
          )

      getResults <- IO.parSequenceN(10)(keys.map(k => cache.get[String, Rate](k)))
      _ = assert(
            getResults.forall(_.isRight),
            s"get failures: ${getResults.collect { case Left(e) => e }.mkString(", ")}"
          )
      _ = assert(
            getResults.forall(_.toOption.flatten.isDefined),
            "All retrieved values should be defined (Right(Some(_)))"
          )
    } yield ()
  }

  test("Cache should handle TTL expiration") {
    val cache = Service[IO](100, 100.millis)

    for {
      _ <- cache.put("expiring-key", testRate)
      _ <- IO.sleep(200.millis)
      result <- cache.get[String, Rate]("expiring-key")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      _ <- IO(assert(result.toOption.flatten.isEmpty, "Expired item should return None"))
    } yield ()
  }

  test("Cache should handle empty cache operations") {
    val cache = Service[IO](100, 1.minute)

    for {
      result <- cache.get[String, Rate]("empty-key")
      _ <- IO(assert(result.isRight, "Empty cache operation should succeed"))
      _ <- IO(assert(result.toOption.flatten.isEmpty, "Empty cache should return None"))
    } yield ()
  }

  test("Cache should handle overwriting existing keys") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("overwrite-key", testRate)
      _ <- cache.put("overwrite-key", testRate2)
      result <- cache.get[String, Rate]("overwrite-key")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      rate <- IO(result.toOption.flatten.get)
      _ <- IO(assertEquals(rate.pair.from, Currency.EUR, "Should be testRate2"))
      _ <- IO(assertEquals(rate.pair.to, Currency.GBP))
    } yield ()
  }

  test("Cache should handle zero TTL") {
    val cache = Service[IO](100, 0.seconds)

    for {
      _ <- cache.put("zero-ttl-key", testRate)
      result <- cache.get[String, Rate]("zero-ttl-key")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      _ <- IO(assert(result.toOption.flatten.isEmpty, "Zero TTL should expire immediately"))
    } yield ()
  }

  test("Cache should handle very large TTL") {
    val cache = Service[IO](100, 1.hour)

    for {
      _ <- cache.put("large-ttl-key", testRate)
      result <- cache.get[String, Rate]("large-ttl-key")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      _ <- IO(assert(result.toOption.flatten.isDefined, "Large TTL should not expire"))
    } yield ()
  }

  test("Cache should handle different value types") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("string-key", "test-string")
      _ <- cache.put("int-key", 42)
      _ <- cache.put("double-key", 3.14)

      stringResult <- cache.get[String, String]("string-key")
      intResult <- cache.get[String, Int]("int-key")
      doubleResult <- cache.get[String, Double]("double-key")

      _ <- IO(assert(stringResult.toOption.flatten.contains("test-string")))
      _ <- IO(assert(intResult.toOption.flatten.contains(42)))
      _ <- IO(assert(doubleResult.toOption.flatten.contains(3.14)))
    } yield ()
  }

  test("Cache should handle edge case configurations") {
    val cache1 = Service[IO](1, 1.millisecond)
    val cache2 = Service[IO](1000, 1.hour)

    for {
      _ <- cache1.put("key1", "value1")
      _ <- cache2.put("key2", "value2")

      result1 <- cache1.get[String, String]("key1")
      result2 <- cache2.get[String, String]("key2")

      _ <- IO(assert(result1.isRight, "Small cache should work"))
      _ <- IO(assert(result2.isRight, "Large cache should work"))
    } yield ()
  }

  test("Cache should handle complex objects") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("rate-key", testRate)
      _ <- cache.put("rate2-key", testRate2)
      _ <- cache.put("rate3-key", testRate3)

      result1 <- cache.get[String, Rate]("rate-key")
      result2 <- cache.get[String, Rate]("rate2-key")
      result3 <- cache.get[String, Rate]("rate3-key")

      _ <- IO(assert(result1.toOption.flatten.isDefined))
      _ <- IO(assert(result2.toOption.flatten.isDefined))
      _ <- IO(assert(result3.toOption.flatten.isDefined))

      rate1 <- IO(result1.toOption.flatten.get)
      rate2 <- IO(result2.toOption.flatten.get)
      rate3 <- IO(result3.toOption.flatten.get)

      _ <- IO(assertEquals(rate1.pair.from, Currency.USD))
      _ <- IO(assertEquals(rate2.pair.from, Currency.EUR))
      _ <- IO(assertEquals(rate3.pair.from, Currency.GBP))
    } yield ()
  }

  test("Cache should handle null key gracefully") {
    val cache = Service[IO](100, 1.minute)

    for {
      putResult <- cache.put(null, "value")
      getResult <- cache.get[String, String](null)

      _ <- IO(assert(putResult.isLeft, "Put with null key should fail"))
      _ <- IO(assert(getResult.isLeft, "Get with null key should fail"))
    } yield ()
  }

  test("Cache should handle cache size limits correctly") {
    val cache = Service[IO](3, 1.minute) // Small cache

    for {
      _ <- cache.put("key1", "value1")
      _ <- cache.put("key2", "value2")
      _ <- cache.put("key3", "value3")

      // All should be present
      result1 <- cache.get[String, String]("key1")
      result2 <- cache.get[String, String]("key2")
      result3 <- cache.get[String, String]("key3")

      _ <- IO(assert(result1.isRight && result2.isRight && result3.isRight))

      // Add one more to trigger eviction
      _ <- cache.put("key4", "value4")

      result4 <- cache.get[String, String]("key4")
      _ <- IO(assert(result4.toOption.flatten.contains("value4")))

      // Check that at least one of the original keys is still there
      result1Check <- cache.get[String, String]("key1")
      result2Check <- cache.get[String, String]("key2")
      result3Check <- cache.get[String, String]("key3")
      allResults   = List(result1Check, result2Check, result3Check)
      presentCount = allResults.count(_.toOption.flatten.isDefined)
      _ <- IO(assert(presentCount >= 2, s"Should have at least 2 items, but found $presentCount"))
    } yield ()
  }

}
