package org.allenai.common

import org.allenai.common.testkit.UnitSpec
import org.allenai.common.StringImplicits._

class StringImplicitsSpec extends UnitSpec {
  "NLPSanitizedString" should "replace unicode characters" in {
    assert("object′s properties".replaceWeirdUnicodeChars === "object's properties")
  }
}
