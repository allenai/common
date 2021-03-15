package org.allenai.common.testkit

import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar
import flatspec._
import matchers._

trait AllenAiBaseSpec extends AnyFlatSpec with should.Matchers with MockitoSugar
