package org.allenai.common

import org.allenai.common.testkit.UnitSpec

class ScalaUtilsSpec extends UnitSpec {

  "ScalaUtils" should "correctly perform Scala operations" in {
    val inputPairs = Seq(("a", 1), ("b", 2), ("a", 3), ("b", 4))
    val outputMap = ScalaUtils.toMapUsingGroupByFirst(inputPairs)
    outputMap("a") should be(List(1, 3))
    outputMap("b") should be(List(2, 4))
  }
}
