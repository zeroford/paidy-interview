package forex.domain.cache

import munit.FunSuite

class FetchStrategySpec extends FunSuite {

  test("FetchStrategy should have MostUsed case object") {
    val strategy = FetchStrategy.MostUsed
    assert(strategy.isInstanceOf[FetchStrategy])
    assertEquals(strategy, FetchStrategy.MostUsed)
  }

  test("FetchStrategy should have All case object") {
    val strategy = FetchStrategy.All
    assert(strategy.isInstanceOf[FetchStrategy])
    assertEquals(strategy, FetchStrategy.All)
  }

  test("MostUsed should have correct toString") {
    assertEquals(FetchStrategy.MostUsed.toString, "MostUsed")
  }

  test("All should have correct toString") {
    assertEquals(FetchStrategy.All.toString, "All")
  }

  test("FetchStrategy case objects should be singleton") {
    assert(FetchStrategy.MostUsed eq FetchStrategy.MostUsed)
    assert(FetchStrategy.All eq FetchStrategy.All)
  }
}
