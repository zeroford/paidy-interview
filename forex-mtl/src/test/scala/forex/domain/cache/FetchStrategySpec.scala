package forex.domain.cache

import munit.FunSuite

class FetchStrategySpec extends FunSuite {

  test("FetchStrategy should provide different strategies") {
    assert(FetchStrategy.MostUsed.isInstanceOf[FetchStrategy])
    assert(FetchStrategy.All.isInstanceOf[FetchStrategy])
  }
}
