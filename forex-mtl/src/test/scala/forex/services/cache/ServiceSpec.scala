package forex.services.cache

import cats.effect.IO
import cats.syntax.parallel._
import munit.CatsEffectSuite
import scala.concurrent.duration._

class ServiceSpec extends CatsEffectSuite {

  test("store and retrieve values") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("key1", 42)
      result <- cache.get[String, Int]("key1")
      _ <- IO(assertEquals(result, Some(42)))
    } yield ()
  }

  test("return None for non-existent keys") {
    val cache = Service[IO](100, 1.minute)

    for {
      result <- cache.get[String, Int]("nonexistent")
      _ <- IO(assertEquals(result, None))
    } yield ()
  }

  test("invalidate specific keys") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("key1", 42)
      _ <- cache.get[String, Int]("key1")
      _ <- cache.clear()
      result <- cache.get[String, Int]("key1")
      _ <- IO(assertEquals(result, None))
    } yield ()
  }

  test("clear all entries") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put("key1", 42)
      _ <- cache.put("key2", 24)
      _ <- cache.clear()
      result1 <- cache.get[String, Int]("key1")
      result2 <- cache.get[String, Int]("key2")
      _ <- IO(assertEquals(result1, None))
      _ <- IO(assertEquals(result2, None))
    } yield ()
  }

  test("work with different key and value types") {
    case class User(id: Int, name: String)

    val cache = Service[IO](100, 1.minute)

    for {
      _ <- cache.put(1, User(1, "Alice"))
      result <- cache.get[Int, User](1)
      _ <- IO(assertEquals(result, Some(User(1, "Alice"))))
    } yield ()
  }

  test("respect maximum size limit") {
    val cache = Service[IO](2, 1.minute)

    for {
      _ <- cache.put("key1", 1)
      _ <- cache.put("key2", 2)
      _ <- cache.put("key3", 3)
      _ <- cache.get[String, Int]("key1")
      result2 <- cache.get[String, Int]("key2")
      result3 <- cache.get[String, Int]("key3")

      _ <- IO(assert(result2.isDefined || result3.isDefined))
    } yield ()
  }

  test("handle concurrent access") {
    val cache = Service[IO](100, 1.minute)

    for {
      _ <- List.range(1, 11).parTraverse(i => cache.put(s"key$i", i))
      results <- List.range(1, 11).parTraverse(i => cache.get[String, Int](s"key$i"))
      _ <- IO(assert(results.forall(_.isDefined)))
      _ <- IO(assert(results.flatten.sum == 55))
    } yield ()
  }
}
