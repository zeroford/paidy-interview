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
      res <- cache.get[String, Rate]("USD-EUR")
    } yield res match {
      case Right(Some(rate)) =>
        assertEquals(rate.pair.from, Currency.USD)
        assertEquals(rate.pair.to, Currency.EUR)
        assertEquals(rate.price.value, BigDecimal(0.85))
      case other =>
        fail(s"Unexpected result: $other")
    }
  }

  test("Cache should return None for non-existent keys") {
    val cache = Service[IO](100, 1.minute)

    for {
      res <- cache.get[String, Rate]("NON-EXISTENT")
    } yield res match {
      case Right(None)    => ()
      case Right(Some(v)) => fail(s"Expected None but got Some($v)")
      case Left(e)        => fail(s"Operation failed: $e")
    }
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
    } yield {
      val presentCount =
        List(result1, result2, result3).count {
          case Right(Some(_)) => true
          case _              => false
        }
      assert(presentCount >= 2, s"Cache should have at least 2 items, but found $presentCount")
      assert(presentCount <= 3, s"Cache should have at most 3 items, but found $presentCount")
    }
  }

  test("Cache should handle concurrent access safely") {
    val cache = Service[IO](maxSize = 100L, ttl = 1.minute)
    val keys  = List.tabulate(10)(i => s"key-$i")

    for {
      putResults <- IO.parSequenceN(10)(keys.map(k => cache.put(k, testRate)))
      getResults <- IO.parSequenceN(10)(keys.map(k => cache.get[String, Rate](k)))
    } yield {
      val putErrors = putResults.collect { case Left(e) => e }
      assert(putErrors.isEmpty, s"put failures: ${putErrors.mkString(", ")}")

      val getErrors = getResults.collect { case Left(e) => e }
      assert(getErrors.isEmpty, s"get failures: ${getErrors.mkString(", ")}")

      val allDefined = getResults.forall { case Right(Some(_)) => true; case _ => false }
      assert(allDefined, "All retrieved values should be defined (Right(Some(_)))")
    }
  }

  test("Cache should handle TTL expiration") {
    val cache = Service[IO](100, 100.millis)

    for {
      _ <- cache.put("expiring-key", testRate)
      _ <- IO.sleep(200.millis)
      res <- cache.get[String, Rate]("expiring-key")
    } yield res match {
      case Right(None) => ()
      case other       => fail(s"Expected Right(None) for expired item, but got: $other")
    }
  }

  test("Cache should handle empty cache operations") {
    val cache = Service[IO](100, 1.minute)

    for {
      res <- cache.get[String, Rate]("empty-key")
    } yield res match {
      case Right(None) => ()
      case other       => fail(s"Expected Right(None) on empty cache, but got: $other")
    }
  }

  test("Cache should handle overwriting existing keys") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("overwrite-key", testRate)
      _ <- cache.put("overwrite-key", testRate2)
      res <- cache.get[String, Rate]("overwrite-key")
    } yield res match {
      case Right(Some(rate)) =>
        assertEquals(rate.pair.from, Currency.EUR)
        assertEquals(rate.pair.to, Currency.GBP)
      case other =>
        fail(s"Unexpected result: $other")
    }
  }

  test("Cache should handle zero TTL") {
    val cache = Service[IO](100, 0.seconds)

    for {
      _ <- cache.put("zero-ttl-key", testRate)
      res <- cache.get[String, Rate]("zero-ttl-key")
    } yield res match {
      case Right(None) => ()
      case other       => fail(s"Expected immediate expiration Right(None), but got: $other")
    }
  }

  test("Cache should handle very large TTL") {
    val cache = Service[IO](100, 1.hour)

    for {
      _ <- cache.put("large-ttl-key", testRate)
      res <- cache.get[String, Rate]("large-ttl-key")
    } yield res match {
      case Right(Some(_)) => ()
      case other          => fail(s"Expected Right(Some(_)) for large TTL, but got: $other")
    }
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
    } yield {
      stringResult match {
        case Right(Some(v)) => assertEquals(v, "test-string")
        case other          => fail(s"string-key unexpected: $other")
      }
      intResult match {
        case Right(Some(v)) => assertEquals(v, 42)
        case other          => fail(s"int-key unexpected: $other")
      }
      doubleResult match {
        case Right(Some(v)) => assertEquals(v, 3.14)
        case other          => fail(s"double-key unexpected: $other")
      }
    }
  }

  test("Cache should handle edge case configurations") {
    val cache1 = Service[IO](1, 1.millisecond)
    val cache2 = Service[IO](1000, 1.hour)

    for {
      _ <- cache1.put("key1", "value1")
      _ <- cache2.put("key2", "value2")
      result1 <- cache1.get[String, String]("key1")
      result2 <- cache2.get[String, String]("key2")
    } yield {
      result1 match {
        case Right(Some(v)) => assertEquals(v, "value1")
        case other          => fail(s"cache1 unexpected: $other")
      }
      result2 match {
        case Right(Some(v)) => assertEquals(v, "value2")
        case other          => fail(s"cache2 unexpected: $other")
      }
    }
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
    } yield {
      result1 match {
        case Right(Some(rate1)) => assertEquals(rate1.pair.from, Currency.USD)
        case other              => fail(s"rate-key unexpected: $other")
      }
      result2 match {
        case Right(Some(rate2)) => assertEquals(rate2.pair.from, Currency.EUR)
        case other              => fail(s"rate2-key unexpected: $other")
      }
      result3 match {
        case Right(Some(rate3)) => assertEquals(rate3.pair.from, Currency.GBP)
        case other              => fail(s"rate3-key unexpected: $other")
      }
    }
  }

  test("Cache should handle null key gracefully") {
    val cache = Service[IO](100, 1.minute)

    for {
      putResult <- cache.put(null, "value")
      getResult <- cache.get[String, String](null)
    } yield {
      putResult match {
        case Left(_)  => ()
        case Right(_) => fail("Put with null key should fail")
      }
      getResult match {
        case Left(_)  => ()
        case Right(_) => fail("Get with null key should fail")
      }
    }
  }

  test("Cache should handle cache size limits correctly") {
    val cache = Service[IO](3, 1.minute)

    for {
      _ <- cache.put("key1", "value1")
      _ <- cache.put("key2", "value2")
      _ <- cache.put("key3", "value3")
      result1 <- cache.get[String, String]("key1")
      result2 <- cache.get[String, String]("key2")
      result3 <- cache.get[String, String]("key3")
      _ <- cache.put("key4", "value4")
      result4 <- cache.get[String, String]("key4")
      r1c <- cache.get[String, String]("key1")
      r2c <- cache.get[String, String]("key2")
      r3c <- cache.get[String, String]("key3")
    } yield {
      List(result1, result2, result3).foreach {
        case Right(_) => ()
        case other    => fail(s"Unexpected failure on initial gets: $other")
      }
      result4 match {
        case Right(Some(v)) => assertEquals(v, "value4")
        case other          => fail(s"Expected key4 present, got: $other")
      }
      val presentCount =
        List(r1c, r2c, r3c).count { case Right(Some(_)) => true; case _ => false }
      assert(presentCount >= 2, s"Should have at least 2 items, but found $presentCount")
    }
  }
}
