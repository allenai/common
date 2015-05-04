package org.allenai.common

import org.allenai.common.testkit.UnitSpec

class ResourceSpec extends UnitSpec {
  val resourceName = "hello.txt"
  val resourcePath = "org/allenai/common/"
  "Resource.get" should "find the correct relative resource" in {
    Resource.get(this.getClass, resourceName)
  }

  "Resource.get" should "find the correct absolute resource" in {
    Resource.get(resourcePath + resourceName)
  }
}
