package org.allenai.common.testkit

/** Base class for any unit test that, for some reason, want to share the Spec instance.
  * Usually the reason is a slow expensive test setup time (setting up a 3rd party DB, etc.)
  */
abstract class SingeInstanceUnitSpec extends AllenAiBaseSpec
