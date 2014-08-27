package org.allenai.common

import org.allenai.common.testkit.UnitSpec
import org.allenai.common.AI2String.implicitConversion

class AI2StringSpec extends UnitSpec {
  "AI2String" should "replace unicode characters" in {
    assert("object′s properties".replaceWeirdChars === "object's properties")
  }
}
