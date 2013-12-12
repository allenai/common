package org.allenai.common

import org.scalatest._

class IntervalSpec extends FlatSpec with Matchers {
  "Artifact" should "have a name and version" in {
    assert(!Artifact.name.isEmpty)
    assert(!Artifact.version.isEmpty)
  }
}
