package org.allenai.common

import scala.concurrent.duration._

/** Methods for measuring the amount of time a method takes. */
object Timing {
  /** Time a unit block and return the duration. */
  def time(block: => Unit): Duration = {
    val start = System.nanoTime()
    block
    val elapsed = System.nanoTime() - start

    elapsed.nanos
  }

  /** Time a block and return a tuple of the result and duration. */
  def time[R](block: => R): (R, Duration) = {
    val start = System.nanoTime()
    val r = block
    val elapsed = System.nanoTime() - start

    (r, elapsed.nanos)
  }

  /** Time a block, run a block using the result, and return the duration. */
  def timeNext[R](block: => R)(next: R => Unit): Duration = {
    val (r, duration) = time(block)
    next(r)
    duration
  }

  /** Time a block, run a block using the result and duration,
    * and return the duration.
    */
  def timeNextBoth[R](block: => R)(next: (R, Duration) => Unit): Duration = {
    val (r, duration) = time(block)
    next(r, duration)
    duration
  }

  /** Time a block, run a block using the duration, and return the result. */
  def timeThen[R](block: => R)(handler: Duration => Unit): R = {
    val (r, duration) = time(block)
    handler(duration)
    r
  }

  /** Time a block, run a block using the result and duration,
    * and return the result.
    */
  def timeThenBoth[R](block: => R)(handler: (R, Duration) => Unit): R = {
    val (r, duration) = time(block)
    handler(r, duration)
    r
  }
}
