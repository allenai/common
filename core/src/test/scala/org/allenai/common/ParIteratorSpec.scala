package org.allenai.common

import java.util.concurrent.atomic.AtomicInteger

import org.allenai.common.testkit.UnitSpec
import org.allenai.common.ParIterator.ParIteratorEnrichment

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ParIteratorSpec extends UnitSpec {
  "ParForeachIterator" should "do things concurrently" in {
    val successes = synchronized(collection.mutable.Set[Int]())

    val iter = Iterator(3, 1, 2)
    val time = Timing.time {
      iter.parForeach { i =>
        Thread.sleep(i * 1000)
        successes.add(i)
      }
    }

    assert(successes === Set(1, 2, 3))
    assert(time < (3500 millis))
  }

  it should "do a great many things concurrently" in {
    val successes = synchronized(collection.mutable.Set[Int]())

    val max = 2000
    val iter = Range(0, max).toIterator
    iter.parForeach { i =>
      Thread.sleep((max - i) % 100)
      successes += i
    }
    val expected = Range(0, max).toSet

    assert((successes -- expected) === Set.empty)
    assert((expected -- successes) === Set.empty)
  }

  it should "nest properly" in {
    val count = new AtomicInteger()
    val max = 13
    Range(0, max).toIterator.parForeach { _ =>
      Range(0, max).toIterator.parForeach { _ =>
        val successes = synchronized(collection.mutable.Set[Int]())

        val iter = Range(0, max).toIterator
        iter.parForeach { i =>
          Thread.sleep((i * max * max) % 100)
          successes += i
          count.incrementAndGet()
        }
        val expected = Range(0, max).toSet

        assert((successes -- expected) === Set.empty)
        assert((expected -- successes) === Set.empty)
      }
    }

    assert(count.get() === max * max * max)
  }

  it should "map things concurrently" in {
    val max = 5
    val values = Range(0, max).reverse
    val iter = values.toIterator
    val expected = values.map { i => s"$i" }
    val time = Timing.time {
      val result = iter.parMap { i =>
        Thread.sleep(i * 1000)
        s"$i"
      }
      assert(expected === result.toSeq)
    }

    assert(time < (max seconds) + (500 millis))
  }

  it should "map lots of things concurrently" in {
    val max = 50000
    val values = Range(0, max).reverse
    val iter = values.toIterator
    val expected = values.map { i => s"$i" }
    val result = iter.parMap { i => s"$i" }
    assert(expected === result.toSeq)
  }
}
