package org.allenai.common

import org.allenai.common.testkit.UnitSpec
import org.allenai.common.StringUtils.StringImplicits

class StringUtilsSpec extends UnitSpec {
  "StringUtils" should "title case only all-caps when required" in {
    "My Title".titleCaseIfAllCaps() should equal("My Title")
    "MY TITLE".titleCaseIfAllCaps() should equal("My Title")
    "".titleCaseIfAllCaps() should equal("")
    "MY title".titleCaseIfAllCaps() should equal("MY title")
  }

  it should "replace unicode characters" in {
    assert("object′s properties".replaceFancyUnicodeChars === "object's properties")
  }
}
