package org.allenai.common

import java.io.InputStream

/** Creates an Iterator from an InputStream.
  * The InputStream is automatically closed once the Iterator has been fully consumed.
  * Example:
  * {{{
  * StreamClosingIterator(new FileInputStream("foo.txt"))(Source.fromInputStream(_).getLines())
  * }}}
  */
object StreamClosingIterator {
  def apply[T](is: InputStream)(makeIterator: InputStream => Iterator[T]): Iterator[T] = {
    val it = makeIterator(is)
    new Iterator[T] {
      private var stillReading = it.hasNext

      override def next(): T = {
        val result = it.next()
        stillReading = it.hasNext
        if (!stillReading) is.close()
        result
      }

      override def hasNext: Boolean = stillReading
    }
  }
}
