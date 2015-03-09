package org.allenai.common

import java.util.concurrent.Semaphore

import scala.concurrent._
import scala.concurrent.duration.Duration

object ParIterator {
  val queueLimit = 1024

  implicit class ParIteratorEnrichment[T](val input: TraversableOnce[T]) extends AnyVal {

    def parForeach(f: T => Unit)(implicit ec: ExecutionContext): Unit = {
      // If there are a billion items in the iterator, we don't want to create a billion futures,
      // so we limit the number of futures we create with this semaphore.
      val sema = new Semaphore(queueLimit)
      input.foreach { item =>
        // Try to pass it off to a future. If no futures are available, do the work
        // in this thread.
        val success = sema.tryAcquire()
        if (success) {
          Future {
            try {
              f(item)
            } finally {
              sema.release()
            }
          }
        } else {
          f(item)
        }
      }
      // wait for all threads to be done
      blocking { sema.acquire(queueLimit) }
    }

    def parMap[O](f: T => O)(implicit ec: ExecutionContext): TraversableOnce[O] = new Iterator[O] {
      private val inner = input.toIterator
      private val q = new scala.collection.mutable.Queue[Future[O]]()

      private def fillQueue(): Unit = {
        while (input.toIterator.hasNext && q.size < queueLimit) {
          val item = inner.next()
          q.enqueue(Future {
            f(item)
          })
        }
      }
      fillQueue()

      override def next(): O = {
        // In Scala, this case is undefined, so we do what the Java spec says.
        if (!hasNext) throw new NoSuchElementException()
        val result = q.dequeue()
        fillQueue()
        Await.result(result, Duration.Inf)
      }

      override def hasNext: Boolean = inner.hasNext || q.nonEmpty
    }
  }
}
