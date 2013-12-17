package org.allenai.common

import scala.concurrent.duration._

/** Methods for measuring the amount of time a method takes. */
object Timing {
  def time[R](block: =>Unit): Duration = {
    val start = System.nanoTime()
    block
    val elapsed = System.nanoTime() - start

    elapsed.nanos
  }

  def time[R](block: =>R): (R, Duration) = {
    val start = System.nanoTime()
    val r = block
    val elapsed = System.nanoTime() - start

    (r, elapsed.nanos)
  }

  def timeNext[R](block: =>R)(next: R=>Unit): Duration = {
    val (r, duration) = time(block)
    next(r)
    duration
  }

  def timeNextBoth[R](block: =>R)(next: (R, Duration)=>Unit): Duration = {
    val (r, duration) = time(block)
    next(r, duration)
    duration
  }

  def timeThen[R](block: =>R)(handler: Duration=>Unit): R = {
    val (r, duration) = time(block)
    handler(duration)
    r
  }

  def timeThenBoth[R](block: =>R)(handler: (R, Duration)=>Unit): R = {
    val (r, duration) = time(block)
    handler(r, duration)
    r
  }
}
