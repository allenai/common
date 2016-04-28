package org.allenai.common

import org.allenai.common.testkit.UnitSpec

class MathUtilsSpec extends UnitSpec {

  "MathUtils" should "correctly perform math operations" in {
    MathUtils.round(3.1415, 2) should be(3.14)
    MathUtils.round(3.1415, 3) should be(3.142)
  }

  it should "correctly incorporate rounding mode" in {
    MathUtils.round(3.1415, 3, BigDecimal.RoundingMode.HALF_DOWN) should be(3.141)
    MathUtils.round(3.1415, 3, BigDecimal.RoundingMode.HALF_UP) should be(3.142)
  }
}
