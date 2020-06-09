package org.allenai.common

import Compat.JavaConverters._
import Compat.IterableOps.IterableOpsImplicits
import org.allenai.common.testkit.UnitSpec
import org.allenai.common.ParIterator.ParIteratorEnrichment

import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ParIteratorSpec extends UnitSpec {
  // With small values (<1000) for scale, this test is unreliable, and with large ones it takes
  // over three seconds, so we ignore it.
  "ParForeachIterator" should "do things concurrently" ignore {
    val successes = new ConcurrentSkipListSet[Int]()

    val scale = 1000

    val iter = Iterator(3, 1, 2)
    val time = Timing.time {
      iter.parForeach { i =>
        Thread.sleep(i * scale)
        successes.add(i)
      }
    }

    assert(successes.asScala === Set(1, 2, 3))
    assert(time < (((3 * scale) + (scale / 2)) millis))
  }

  it should "do a great many things concurrently" in {
    Iterator.fill(10)(0).foreach { _ =>
      val successes = new ConcurrentSkipListSet[Int]()

      val max = 2000
      val iter = Range(0, max).toIteratorCompat
      iter.parForeach { i =>
        Thread.sleep((max - i) % 10)
        successes.add(i)
      }
      val expected = Range(0, max).toSet

      assert((successes.asScala.toSet -- expected) === Set.empty)
      assert((expected -- successes.asScala.toSet) === Set.empty)

      Thread.sleep(1000)

      assert((successes.asScala.toSet -- expected) === Set.empty)
      assert((expected -- successes.asScala.toSet) === Set.empty)
    }
  }

  it should "nest properly" in {
    val count = new AtomicInteger()
    val max = 13
    Range(0, max).toIteratorCompat.parForeach { _ =>
      Range(0, max).toIteratorCompat.parForeach { _ =>
        val successes = new ConcurrentSkipListSet[Int]()

        val iter = Range(0, max).toIteratorCompat
        iter.parForeach { i =>
          Thread.sleep((i * max * max) % 10)
          successes.add(i)
          count.incrementAndGet()
        }
        val expected = Range(0, max).toSet

        assert((successes.asScala.toSet -- expected) === Set.empty)
        assert((expected -- successes.asScala.toSet) === Set.empty)
      }
    }

    assert(count.get() === max * max * max)
  }

  it should "map things concurrently" in {
    val max = 5
    val values = Range(0, max).reverse
    val iter = values.toIteratorCompat
    val expected = values.map { i =>
      s"$i"
    }
    val time: Duration = Timing.time {
      val result = iter.parMap { i =>
        Thread.sleep(i * 100)
        s"$i"
      }
      assert(expected === result.toSeq)
    }

    val limit: Duration = ((max * 100) millis) + (50 millis)
    assert(time < limit)
  }

  it should "map lots of things concurrently" in {
    val max = 50000
    val values = Range(0, max).reverse
    val iter = values.toIteratorCompat
    val expected = values.map { i =>
      s"$i"
    }
    val result = iter.parMap { i =>
      s"$i"
    }
    assert(expected === result.toSeq)
  }

  it should "return exceptions from foreach functions" in {
    val successes = synchronized(collection.mutable.Set[Int]())
    intercept[ArithmeticException] {
      Range(-20, 20).toIteratorCompat.parForeach { i =>
        successes.add(10000 / i)
      }
    }
  }

  it should "return the first exception from foreach functions" in {
    intercept[ArithmeticException] {
      Iterator(new NotImplementedError(), new ArithmeticException()).zipWithIndex.parForeach {
        case (e, index) =>
          Thread.sleep((1 - index) * 1000)
          throw e
      }
    }
  }

  it should "return exceptions from map" in {
    intercept[ArithmeticException] {
      Range(-20, 20).toIteratorCompat.parMap(10000 / _).toList
    }
  }
}
