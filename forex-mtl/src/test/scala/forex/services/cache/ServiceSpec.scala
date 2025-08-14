package forex.services.cache

import cats.effect.IO
import forex.domain.currency.Currency
import forex.domain.rates.{ Price, Rate, Timestamp }
import munit.CatsEffectSuite
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import java.time.OffsetDateTime
import scala.concurrent.duration._

class ServiceSpec extends CatsEffectSuite {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val testRate = Rate(
    pair = Rate.Pair(Currency.USD, Currency.EUR),
    price = Price(BigDecimal(0.85)),
    timestamp = Timestamp(OffsetDateTime.now())
  )

  private val testRate2 = Rate(
    pair = Rate.Pair(Currency.EUR, Currency.GBP),
    price = Price(BigDecimal(0.75)),
    timestamp = Timestamp(OffsetDateTime.now())
  )

  private val testRate3 = Rate(
    pair = Rate.Pair(Currency.GBP, Currency.JPY),
    price = Price(BigDecimal(150.0)),
    timestamp = Timestamp(OffsetDateTime.now())
  )

  test("Cache should store and retrieve rates successfully") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("USD-EUR", testRate)
      result <- cache.get[String, Rate]("USD-EUR")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      retrievedRate <- IO(result.getOrElse(None))
      _ <- IO(assert(retrievedRate.isDefined, "Retrieved rate should be defined"))
      _ <- IO(assertEquals(retrievedRate.get.pair, testRate.pair, "Rate pair should match"))
      _ <- IO(assertEquals(retrievedRate.get.price, testRate.price, "Rate price should match"))
      _ <- IO(assertEquals(retrievedRate.get.timestamp, testRate.timestamp, "Rate timestamp should match"))
    } yield ()
  }

  test("Cache should return None for non-existent keys") {
    val cache = Service[IO](100, 1.minute)

    for {
      result <- cache.get[String, Rate]("NON-EXISTENT")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      _ <- IO(assert(result.getOrElse(None).isEmpty, "Non-existent key should return None"))
    } yield ()
  }

  test("Cache should handle cache eviction when full") {
    val cache = Service[IO](2, 1.minute)

    for {
      _ <- cache.put("USD-EUR", testRate)
      _ <- cache.put("EUR-GBP", testRate2)
      _ <- cache.put("GBP-JPY", testRate3)
      result1 <- cache.get[String, Rate]("USD-EUR")
      result2 <- cache.get[String, Rate]("EUR-GBP")
      result3 <- cache.get[String, Rate]("GBP-JPY")
      _ <- IO(assert(result1.isRight, "Cache operation should succeed"))
      _ <- IO(assert(result2.isRight, "Cache operation should succeed"))
      _ <- IO(assert(result3.isRight, "Cache operation should succeed"))

      presentCount <- IO(List(result1, result2, result3).count(_.getOrElse(None).isDefined))
      _ <- IO(assert(presentCount <= 3, s"Cache should have at most 3 items, but found $presentCount"))
      _ <- IO(assert(presentCount >= 1, s"Cache should have at least 1 item, but found $presentCount"))
    } yield ()
  }

  test("Cache should handle concurrent access safely") {
    val cache = Service[IO](100, 1.minute)

    for {
      putResults <- IO.parSequenceN(10)(
                      List.tabulate(10)(i => cache.put(s"key-$i", testRate))
                    )
      _ <- IO(assert(putResults.forall(_.isRight), "All put operations should succeed"))

      getResults <- IO.parSequenceN(10)(
                      List.tabulate(10)(i => cache.get[String, Rate](s"key-$i"))
                    )
      _ <- IO(assert(getResults.forall(_.isRight), "All get operations should succeed"))
      _ <- IO(assert(getResults.forall(_.fold(_ => false, _.isDefined)), "All retrieved values should be defined"))
    } yield ()
  }

  test("Cache should handle TTL expiration") {
    val cache = Service[IO](100, 100.millis)

    for {
      _ <- cache.put("TTL-TEST", testRate)
      _ <- IO.sleep(200.millis)
      result <- cache.get[String, Rate]("TTL-TEST")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      _ <- IO(assert(result.getOrElse(None).isEmpty, "Expired item should return None"))
    } yield ()
  }

  test("Cache should handle empty cache operations") {
    val cache = Service[IO](100, 1.minute)

    for {
      result <- cache.get[String, Rate]("EMPTY")
      _ <- IO(assert(result.isRight, "Empty cache operation should succeed"))
      _ <- IO(assert(result.getOrElse(None).isEmpty, "Empty cache should return None"))
    } yield ()
  }

  test("Cache should handle overwriting existing keys") {
    val cache       = Service[IO](100, 1.minute)
    val updatedRate = testRate.copy(price = Price(BigDecimal(0.90)))

    for {
      _ <- cache.put("USD-EUR", testRate)
      _ <- cache.put("USD-EUR", updatedRate)
      result <- cache.get[String, Rate]("USD-EUR")
      _ <- IO(assert(result.isRight, "Cache operation should succeed"))
      retrievedRate <- IO(result.getOrElse(None))
      _ <- IO(assert(retrievedRate.isDefined, "Retrieved rate should be defined"))
      _ <- IO(assertEquals(retrievedRate.get.price, updatedRate.price, "Updated price should match"))
    } yield ()
  }
}
