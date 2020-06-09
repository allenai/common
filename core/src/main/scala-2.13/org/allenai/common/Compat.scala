package org.allenai.common

import scala.collection.convert._

object Compat {
  object JavaConverters extends AsJavaExtensions with AsScalaExtensions

  object IterableOps {
    implicit class IterableOpsImplicits[A](iter: Iterable[A]) {
      def toStreamCompat: LazyList[A] = iter.to(LazyList)
      def toIteratorCompat: Iterator[A] = iter.iterator
    }
  }
}
