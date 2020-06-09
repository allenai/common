package org.allenai.common

import scala.collection.convert._

object Compat {
  object JavaConverters extends DecorateAsJava with DecorateAsScala

  object IterableOps {
    implicit class IterableOpsImplicits[A](iter: Iterable[A]) {
      def toStreamCompat: Stream[A] = iter.toStream
      def toIteratorCompat: Iterator[A] = iter.toIterator
    }
  }
}
