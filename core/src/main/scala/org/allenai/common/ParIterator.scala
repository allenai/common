package org.allenai.common

import java.util.concurrent.{ TimeUnit, Semaphore }
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

object ParIterator {
  val defaultQueueLimit = 1024

  implicit class ParIteratorEnrichment[T](val input: Iterator[T]) extends AnyVal {

    /** Runs the given function over all values from the iterator in parallel, and returns when they
      * are all done.
      *
      * The maximum parallelism that's supported by the execution context applies. Also, this
      * function takes care not to overload the execution context with more requests than it can
      * handle.
      *
      * If one or more of the function executions throw an exception, parForeach throws that same
      * exception. It is undefined which exception gets thrown. The iterator is left in an
      * undefined state in this case.
      *
      * @param f  the function to execute
      * @param ec the execution context to run the function executions in
      */
    def parForeach(
      f: T => Unit,
      queueLimit: Int = defaultQueueLimit
    )(implicit ec: ExecutionContext): Unit = {
      // If there are a billion items in the iterator, we don't want to create a billion futures,
      // so we limit the number of futures we create with this semaphore.
      val sema = new Semaphore(queueLimit)

      val firstException = new AtomicReference[Option[Throwable]](None)

      while (input.hasNext && firstException.get().isEmpty) {
        val item = input.next()

        // Try to pass it off to a future. If no futures are available, do the work in this thread.
        val success = sema.tryAcquire()
        if (success) {
          Future {
            try {
              f(item)
            } catch {
              case NonFatal(e) =>
                val success = firstException.compareAndSet(None, Some(e))
                // If we didn't set the exception, rethrow so that any potential uncaught
                // exceptions handlers have a chance to get this one.
                if (!success) throw e
            } finally {
              sema.release()
            }
          }
        } else {
          f(item)
        }
      }

      // wait for all threads to be done
      var success = false
      while (firstException.get().isEmpty && !success) {
        blocking {
          success = sema.tryAcquire(queueLimit, 1000, TimeUnit.MILLISECONDS)
        }
      }

      // throw first exception if there is one
      firstException.get().foreach { e => throw e }
    }

    /** Maps an iterator to another iterator, performing the maps on the elements in parallel.
      * Returns an iterator with the values mapped by the given function.
      *
      * The maximum parallelism that's supported by the execution context applies. Also, this
      * function makes sure to not overwhelm the JVM's resources with too many temporary results. To
      * do this, the returned iterator will "work ahead" of the thread that's reading from it, but
      * there is a limit to how far ahead it will work.
      *
      * If one or more of the map function executions throw an exception, only the first exception
      * is reported, and it is reported when you call next() on the returned iterator.
      *
      * @param f  the function performing the mapping
      * @param ec the execution context to run the function executions in
      * @tparam O the type of the output
      * @return   a new iterator with the mapped values from the old iterator
      */
    def parMap[O](
      f: T => O,
      queueLimit: Int = defaultQueueLimit
    )(implicit ec: ExecutionContext): Iterator[O] = new Iterator[O] {
      private val inner = input.toIterator
      private val q = new scala.collection.mutable.Queue[Future[O]]()

      private def fillQueue(): Unit = {
        while (inner.hasNext && q.size < queueLimit) {
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
