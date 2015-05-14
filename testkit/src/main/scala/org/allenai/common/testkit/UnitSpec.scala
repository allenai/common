package org.allenai.common.testkit

import org.scalatest.OneInstancePerTest

/** Base class for any unit test.
  * Runs each test in a separate Spec instance, to simplify / guarantee test isolation.
  * This is the default behavior of JUnit.
  */
abstract class UnitSpec extends AllenAiBaseSpec with OneInstancePerTest
