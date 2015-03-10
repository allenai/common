package org.allenai.common

import java.util.concurrent.Semaphore
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
      * If one or more of the function executions throw an exception, only the first exception is
      * reported. The iterator is still exhausted, and all other executions are attempted. That's
      * impractical, but consistent with the behavior of the parallel collections in Scala.
      *
      * @param f  the function to execute
      * @param ec the execution context to run the function executions in
      */
    def parForeach(f: T => Unit, queueLimit: Int = defaultQueueLimit)(implicit ec: ExecutionContext): Unit = {
      // If there are a billion items in the iterator, we don't want to create a billion futures,
      // so we limit the number of futures we create with this semaphore.
      val sema = new Semaphore(queueLimit)

      val firstException = new AtomicReference[Option[(Int, Throwable)]](None)
      def setException(index: Int, exception: Throwable): Unit = {
        var success = false
        while (!success) {
          val current = firstException.get()
          current match {
            case None =>
              success = firstException.compareAndSet(None, Some((index, exception)))
            case Some((oldIndex: Int, _)) if index < oldIndex =>
              success = firstException.compareAndSet(current, Some((index, exception)))
            case _ =>
              success = true
          }
        }
      }

      input.zipWithIndex foreach {
        case (item, index) =>
          // Try to pass it off to a future. If no futures are available, do the work
          // in this thread.
          val success = sema.tryAcquire()
          if (success) {
            Future {
              try {
                f(item)
              } catch {
                case NonFatal(e) => setException(index, e)
              } finally {
                sema.release()
              }
            }
          } else {
            try {
              f(item)
            } catch {
              case NonFatal(e) => setException(index, e)
            }
          }
      }

      // wait for all threads to be done
      blocking { sema.acquire(queueLimit) }

      // throw first exception if there is one
      firstException.get().foreach { case (_, e) => throw e }
    }

    /** Maps an iterator to another iterator, performing the maps on the elements in parallel.
      * Returns an iterator with the values mapped by the given function.
      *
      * The maximum parallelism that's supported by the execution context applies. Also, this
      * function makes sure to not overwhelm the JVM's resources with too many temporary results. To
      * do this, the returned iterator will "work ahead" of the thread that's reading from it, but
      * there is a limit to how far ahead it will work.
      *
      * If one or more of the map function executions throw an exception, only the first exception is
      * reported, and it is reported when you call next() on the returned iterator.
      *
      * @param f  the function performing the mapping
      * @param ec the execution context to run the function executions in
      * @tparam O the type of the output
      * @return   a new iterator with the mapped values from the old iterator
      */
    def parMap[O](f: T => O, queueLimit: Int = defaultQueueLimit)(implicit ec: ExecutionContext): TraversableOnce[O] = new Iterator[O] {
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
