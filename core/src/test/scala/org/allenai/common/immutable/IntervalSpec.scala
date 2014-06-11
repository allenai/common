/** Adapted from BSD software developed by Michael Schmitz
  * at the the University of Washington.
  *
  * https://github.com/knowitall/common-scala
  *
  *
  * Copyright (c) 2012, University of Washington
  * BSD 3-clause License / BSD Modified License / New BSD License
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  * Redistributions of source code must retain the above copyright
  * notice, this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright
  * notice, this list of conditions and the following disclaimer in the
  * documentation and/or other materials provided with the distribution.
  * Neither the name of the University of Washington nor the
  * names of its contributors may be used to endorse or promote products
  * derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF WASHINGTON BE LIABLE FOR ANY
  * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  */

package org.allenai.common.immutable

import org.allenai.common.testkit.UnitSpec

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.propBoolean
import org.scalatest.prop.Checkers

class IntervalSpec extends UnitSpec with Checkers {
  they should "border each other" in {
    assert((Interval.open(0, 4) borders Interval.open(4, 8)) == true)
    assert((Interval.open(4, 8) borders Interval.open(0, 4)) == true)
    assert((Interval.open(0, 3) borders Interval.open(4, 8)) == false)
    assert((Interval.open(4, 8) borders Interval.open(0, 3)) == false)
    assert((Interval.empty borders Interval.open(4, 8)) == false)
  }

  they should "union properly" in {
    assert((Interval.open(0, 4) union Interval.open(4, 8)) == (Interval.open(0, 8)))
    intercept[IllegalArgumentException] {
      (Interval.open(0, 4) union Interval.open(6, 8))
    }
  }

  they should "intersect properly" in {
    assert((Interval.open(0, 4) intersect Interval.open(4, 8)) == (Interval.empty))
    assert((Interval.open(0, 4) intersect Interval.open(6, 8)) == (Interval.empty))
    assert((Interval.open(0, 4) intersect Interval.open(2, 6)) == (Interval.open(2, 4)))
  }

  they should "contain properly" in {
    assert((Interval.open(2, 3) contains 0) == false)
    assert((Interval.open(2, 3) contains 1) == false)
    assert((Interval.open(2, 3) contains 2) == true)
    assert((Interval.open(2, 3) contains 3) == false)
  }

  they should "shift ok" in {
    assert((Interval.open(2, 4) shift 2) == Interval.open(4, 6))
    assert((Interval.open(2, 4) shift -2) == Interval.open(0, 2))
  }

  "the correct left interval" should "be determined" in {
    assert((Interval.open(0, 4) left Interval.open(4, 8)) == (Interval.open(0, 4)))
    assert((Interval.open(0, 4) left Interval.open(2, 6)) == (Interval.open(0, 4)))
    assert((Interval.open(4, 8) left Interval.open(0, 4)) == (Interval.open(0, 4)))
    assert((Interval.open(2, 6) left Interval.open(0, 4)) == (Interval.open(0, 4)))
  }

  "the correct right interval" should "be determined" in {
    assert((Interval.open(0, 4) right Interval.open(4, 8)) == (Interval.open(4, 8)))
    assert((Interval.open(0, 4) right Interval.open(2, 6)) == (Interval.open(2, 6)))
    assert((Interval.open(4, 8) right Interval.open(0, 4)) == (Interval.open(4, 8)))
    assert((Interval.open(2, 6) right Interval.open(0, 4)) == (Interval.open(2, 6)))
  }

  "leftOf" should "work" in {
    assert((Interval.open(0, 4) leftOf Interval.open(4, 8)) == true)
    assert((Interval.open(0, 4) leftOf Interval.open(2, 6)) == false)
    assert((Interval.open(4, 8) leftOf Interval.open(0, 4)) == false)
    assert((Interval.open(2, 6) leftOf Interval.open(0, 4)) == false)
  }

  "rightOf" should "work" in {
    assert((Interval.open(0, 4) rightOf Interval.open(4, 8)) == false)
    assert((Interval.open(0, 4) rightOf Interval.open(2, 6)) == false)
    assert((Interval.open(4, 8) rightOf Interval.open(0, 4)) == true)
    assert((Interval.open(2, 6) rightOf Interval.open(0, 4)) == false)
  }

  "overlapping intervals" should "have distance 0" in {
    assert((Interval.open(0, 4) distance Interval.open(2, 6)) == (0))
    assert((Interval.open(2, 6) distance Interval.open(0, 3)) == (0))
  }

  they should "have the correct distance" in {
    assert((Interval.open(0, 2) distance Interval.open(2, 5)) == (1))
    assert((Interval.open(0, 2) distance Interval.open(3, 5)) == (2))
    assert((Interval.open(0, 2) distance Interval.open(4, 6)) == (3))
  }

  "adjacent intervals" should "have the empty set between them" in {
    assert(Interval.between(Interval.open(0, 2), Interval.open(2, 3)) == (Interval.empty))
  }

  "between" should "work properly" in {
    assert(Interval.between(Interval.open(0, 2), Interval.open(3, 10)) == (Interval.open(2, 3)))
    assert(Interval.between(Interval.open(0, 2), Interval.open(6, 10)) == (Interval.open(2, 6)))
  }

  val intervalGen = for {
    n <- Gen.choose(0, 100)
    m <- Gen.choose(n, 100)
  } yield Interval.open(n, m)

  "Interval.minimal" should "work properly" in {
    implicit def arbInterval: Arbitrary[List[Interval]] = {
      Arbitrary {
        Gen.listOf(intervalGen)
      }
    }

    forAll { (intervals: List[Interval]) =>
      val min = Interval.minimal(intervals)

      // for all points in the original intervals
      // that point must be in the new intervals
      intervals.forall(i => min.exists(_.contains(i)))

      // for all points in one of the new intervals
      // no other interval may contain the same point
      min.forall(interval => !min.exists(other => !(other eq interval) && (other intersects interval)))

      // result is sorted
      min.sorted == min
    }
  }

  "empty" should "work properly" in {
    assert((Interval.empty union Interval.open(2, 4)) == (Interval.open(2, 4)))
    assert((Interval.empty intersect Interval.open(2, 4)) == (Interval.empty))

    assert((Interval.empty left Interval.open(2, 4)) == (Interval.open(2, 4)))
    assert((Interval.open(2, 4) left Interval.empty) == (Interval.open(2, 4)))
    assert((Interval.empty right Interval.open(2, 4)) == (Interval.open(2, 4)))
    assert((Interval.open(2, 4) right Interval.empty) == (Interval.open(2, 4)))

    assert((Interval.open(2, 4) subset Interval.empty) == false)
    assert((Interval.empty subset Interval.open(2, 4)) == true)

    assert((Interval.open(2, 4) superset Interval.empty) == true)
    assert((Interval.empty superset Interval.open(2, 4)) == false)

    intercept[IllegalArgumentException] { Interval.empty.min }
    intercept[IllegalArgumentException] { Interval.empty.max }
    intercept[IllegalArgumentException] { Interval.empty leftOf Interval.open(2, 4) }
    intercept[IllegalArgumentException] { Interval.open(2, 4) rightOf Interval.empty }

    assert(Interval.empty.shift(5) == Interval.empty)
  }

  def roundtripString(s: String): String = Interval.Format.write(Interval.Format.read(s))
  def roundtrip(x: Interval): Interval = Interval.Format.read(Interval.Format.write(x))

  def roundtripsOkString(s: String): Unit = assert(roundtripString(s) == s)
  def roundtripsOk(x: Interval): Unit = assert(roundtrip(x) == x)
  "empty" should "round trip through serialization" in {
    roundtripsOk(Interval.empty)
  }

  "singleton intervals" should "round trip through serialization" in {
    check { (x: Int) =>
      (x < Int.MaxValue) ==> {
        val interval: Interval = Interval.singleton(x)
        Interval.closed(x, x) == interval
      }
    }

    check { (x: Int) =>
      (x < Int.MaxValue) ==> {
        val interval: Interval = Interval.singleton(x)
        roundtrip(interval) == interval
      }
    }
  }

  "open intervals" should "round trip through serialization" in {
    forAll { (a: Int, b: Int) =>
      (a < b) ==> {
        val interval = Interval.open(a, b)
        roundtrip(interval) == interval
      }
    }
  }

  "closed intervals" should "round trip through serialization" in {
    check { (a: Int, b: Int) =>
      (a <= b && b < Int.MaxValue) ==> {
        val interval = Interval.closed(a, b)
        roundtrip(interval) == interval
      }
    }
  }
}
