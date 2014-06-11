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
  * DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF WASHINGTON BE LIABLE FOR ANY
  * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  */

package org.allenai.common.immutable

import Interval.empty
import scala.util.matching.Regex

/** Represents an open interval in the Integers.
  *
  * Intervals are created using the companion object.
  *
  * @param  start  the first item in this interval
  * @param  end  one past the last item in this interval
  *
  * @author  Michael Schmitz
  */
sealed class Interval private (val start: Int, val end: Int)
    extends IndexedSeq[Int] with Ordered[Interval] {
  import Interval._
  require(start <= end, "start must be <= end: " + start + ">" + end)

  override def toString = "[" + start + ", " + end + ")"
  override def equals(that: Any) = that match {
    // fast comparison for Intervals
    case that: Interval => that.canEqual(this) && that.start == this.start && that.end == this.end
    // slower comparison for Seqs
    case that: IndexedSeq[_] => super.equals(that)
    case _ => false
  }
  override def canEqual(that: Any) = that.isInstanceOf[Interval]
  override def compare(that: Interval) =
    if (this.start > that.start) 1
    else if (this.start < that.start) -1
    else this.length - that.length

  /** Return the ith value of the interval.
    *
    * @param  index  the index to get
    * @return  the ith value of the interval
    */
  override def apply(index: Int): Int = {
    require(index >= 0, "index < 0: " + index)
    require(index < length, "index >= length: " + index + " >= " + length)

    // a fast way of getting the ith index
    min + index
  }

  override def iterator: Iterator[Int] = {
    new Iterator[Int] {
      var index = start

      def hasNext = index < end
      def next() = {
        val result = index
        index += 1
        result
      }
    }
  }

  override def seq = this

  /** The length of the interval. */
  override def length = end - start

  /** Tests whether this list contains a given value as an element.
    *
    * @param  x  the value to check
    * @return  true if this interval contains `x`
    */
  def contains(x: Int) = x >= start && x < end

  /** Tests whether two intervals border but do not overlap.
    *
    * @param  that  the interval to check
    * @return  true if this interval borders the other interval
    */
  def borders(that: Interval) = {
    if (this == empty || that == empty) false
    else that.max == this.min - 1 || that.min == this.max + 1
  }

  /** Tests whether a point border an interval.
    *
    * @param  that  the point to check
    * @return  true if this interval borders the point
    */
  def borders(that: Int) = {
    if (this == empty) false
    else this.start - 1 == that || this.end == that
  }

  /** Tests whether this interval is a superset of another interval.
    *
    * @param  that  the interval to check
    * @return  true if `this` is a superset of `that`
    */
  def superset(that: Interval) = {
    if (that == empty) true
    else if (this == empty) false
    else this.start <= that.start && this.end >= that.end
  }

  /** Tests whether this interval is a subsert of another interval.
    *
    * @param  that  the interval to check
    * @return  true if `this` is a subset of `that`
    */
  def subset(that: Interval) = {
    if (that == empty) false
    else if (this == empty) true
    else this.start >= that.start && this.end <= that.end
  }

  /** Tests whether another interval intersects this interval.
    *
    * @param  that  the interval to check
    * @return  true if `this` intersects `that`
    */
  def intersects(that: Interval) = {
    if (that == empty || this == empty) false
    else if (this == that) true
    else {
      val left = this left that
      val right = this right that
      left.end > right.start
    }
  }

  /** Tests whether another interval is disjoint from this interval.
    * This is the opposite of `intersects`.
    *
    * @param  that  the interval to check
    * @return  true if `this` is disjoint from `that`
    */
  def disjoint(that: Interval) = !this.intersects(that)

  /** Measure the distance between two intervals.
    * Bordering intervals have distance 1 and intersecting
    * intervals have distance 0.  The distance is always
    * a positive number.
    *
    * @param  that  the interval to measure against
    * @return  the distance between two intervals.
    */
  def distance(that: Interval) = {
    require(that != empty && this != empty, "empty interval")
    if (this intersects that) 0
    else (this.min max that.min) - (this.max min that.max)
  }

  /** Takes the union of two intervals.
    * The two intervals must border or intersect each other.
    */
  def union(that: Interval) = {
    if (that == empty) this
    else if (this == empty) that
    else {
      require((this borders that) || (this intersects that), "intervals must border or intersect")
      Interval.open(that.start min this.start, that.end max this.end)
    }
  }

  /** Takes the intersection of two intervals, or Interval.empty
    * if they do not intersect.
    */
  def intersect(that: Interval) = {
    if (that == empty || this == empty) Interval.empty
    else {
      val start = this.start max that.start
      val end = this.end min that.end
      if (start < end) Interval.open(start, end)
      else Interval.empty
    }
  }

  /** Move the interval right.
    *
    * @param  by  the distance to move the interval
    */
  def shift(by: Int) = Interval.open(this.start + by, this.end + by)

  /** Returns true if this is left of that.
    */
  def leftOf(that: Interval) = {
    require(that != empty && this != empty, "empty interval")
    this.end <= that.start
  }

  /** Returns true if this is right of that.
    */
  def rightOf(that: Interval) = {
    require(that != empty && this != empty, "empty interval")
    this.start >= that.end
  }

  /** Determine whether this interval or the supplied interval is left.
    * First compare based on the intervals' start, and secondly compare
    * based on the intervals' length.
    */
  def left(that: Interval) =
    if (that == empty) this
    else if (this == empty) that
    else if (that.start < this.start) that
    else if (that.start > this.start) this
    else if (that.length < this.length) that
    else this

  /** Determine whether this interval or the supplied interval is right.
    * First compare based on the intervals' start, and secondly compare
    * based on the intervals' length.
    */
  def right(that: Interval) =
    if (that == empty) this
    else if (this == empty) that
    else if (that.start > this.start) that
    else if (that.start < this.start) this
    else if (that.length > this.length) that
    else this

  /** The minimum index in the interval. */
  def min = {
      require(this != empty, "empty interval")
      start
    }

  /** The maximum index in the interval. */
  def max = {
    require(this != empty, "empty interval")
    end - 1
  }
}

object Interval {
  /** The empty interval. */
  val empty = Empty

  /** Create a new singleton interval. */
  def singleton(x: Int): Singleton = new SingletonImpl(x)

  /** Create a new open interval. */
  def open(start: Int, end: Int): Interval = {
    require(end >= start, "end < start: " + end + " < " + start)
    if (start == end) Interval.empty
    else if (end - start == 1) Interval.singleton(start)
    else new Interval(start, end)
  }

  /** Create a new closed interval. */
  def closed(start: Int, end: Int): Interval = {
    require(end < Int.MaxValue, "end must be < Int.MaxValue")
    require(end >= start, "end < start: " + end + " < " + start)
    if (end == start) Interval.singleton(start)
    else new Closed(start, end)
  }

  /** Create an interval at the specified starting point of the specified length. */
  def ofLength(start: Int, length: Int): Interval = Interval.open(start, start + length)

  object Format {
    val emptyRegex = new Regex("\\{\\}")
    val singletonRegex = new Regex("\\{([+-]?\\d+)\\}")
    val openIntervalRegex = new Regex("\\[([+-]?\\d+), ([+-]?\\d+)\\)")
    val closedIntervalRegex = new Regex("\\[([+-]?\\d+), ([+-]?\\d+)\\]")

    def write(interval: Interval): String = interval.toString

    def read(pickled: String): Interval = {
      pickled match {
        case emptyRegex() => Interval.empty
        case singletonRegex(value) => Interval.singleton(value.toInt)
        case openIntervalRegex(a, b) => Interval.open(a.toInt, b.toInt)
        case closedIntervalRegex(a, b) => Interval.closed(a.toInt, b.toInt)
      }
    }
  }

  /** Create an open interval that includes all points between the two intervals. */
  def between(x: Interval, y: Interval): Interval = {
    require(!(x intersects y), "intervals may not intersect")
    Interval.open(x.end min y.end, x.start max y.start)
  }

  /** create an interval from a sequence of `Int`s.
    *
    * @throws IllegalArgumentException  some x such that min < x < max is not in col
    */
  def from(col: Seq[Int]): Interval = {
    if (col.isEmpty) Interval.empty
    else {
      val sorted = col.sorted
      val min = sorted.head

      require(sorted.zipWithIndex.forall { case (x, i) => x == min + i }, "missing elements in collection: " + col)

      Interval.closed(min, sorted.last)
    }
  }

  /** create an interval from a collection of intervals.  The intervals will be
    * sorted and unioned.
    *
    * @throws IllegalArgumentException  gap in intervals
    */
  def union(col: Seq[Interval]): Interval = {
    val sorted = col.sorted
    try {
      sorted.reduceRight(_ union _)
    } catch {
      case _: IllegalArgumentException => throw new IllegalArgumentException("gap in intervals: " + sorted)
    }
  }

  /** create the smallest interval that spans a collection of intervals.
    * The intervals will be sorted and unioned.
    *
    * @throws IllegalArgumentException  gap in intervals
    */
  def span(col: Iterable[Interval]): Interval = {
    if (col.isEmpty) Interval.empty
    else Interval.open(col.map(_.min).min, col.map(_.max).max + 1)
  }

  /** create a minimal spanning set of the supplied intervals.
    *
    * @return  a sorted minimal spanning set
    */
  def minimal(intervals: Iterable[Interval]): List[Interval] = {
    val set = collection.immutable.SortedSet.empty[Int] ++ intervals.flatten
    set.foldLeft(List.empty[Interval]) {
      case (list, i) =>
        val singleton = Interval.singleton(i)
        list match {
          case Nil => List(singleton)
          case x :: xs if x borders i => (x union singleton) :: xs
          case xs => singleton :: xs
        }
    }.reverse
  }

  // implementations

  object Open {
    /** Match exposing the bounds as an open interval */
    def unapply(interval: Interval): Option[(Int, Int)] = interval match {
      case `empty` => None
      case open: Interval => Some((open.start, open.end))
    }
  }

  /** The empty interval.
    */
  object Empty extends Interval(0, 0) {
    override def toString = "{}"
    def unapply(interval: Interval): Option[Unit] = interval match {
      case `empty` => Some(Unit)
      case _ => None
    }
  }

  /** `ClosedInterval` extends `Open` so it is not a
    * required case when  pattern matching an `Interval`.
    * This is a convenience class for providing a closed toString.
    * However, Open is the default Interval type.
    */
  class Closed private[Interval] (start: Int, end: Int)
      extends Interval(start, end + 1) {
    override def toString = "[" + start + ", " + end + "]"
  }

  object Closed {
    def unapply(interval: Interval): Option[(Int, Int)] = interval match {
      case `empty` => None
      case closed: Interval => Some((interval.start, interval.last))
      case _ => None
    }
  }

  /** An interval that includes only a single index.
    * All intervals with a single element will always extend Singleton.
    */
  sealed abstract class Singleton private[Interval] (elem: Int)
      extends Interval(elem, elem + 1) {
    def index = this.start
    override def toString = "{" + elem + "}"
  }

  object Singleton {
    /** Match exposing the bounds as a singleton */
    def unapply(interval: Interval): Option[Int] = interval match {
      case singleton: Singleton => Some(singleton.index)
      case _ => None
    }
  }

  private class SingletonImpl(elem: Int) extends Singleton(elem)
}
