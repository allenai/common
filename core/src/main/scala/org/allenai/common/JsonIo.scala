package org.allenai.common

import spray.json._

import scala.io.Source

import java.io.{ OutputStream, PrintWriter }

/** Helpers for streaming lists of JSON objects to and from disk. */
object JsonIo {
  /** Reads single-lines from a given Source, and streams the JSON parsed from them to the caller.
    * @return a stream of objects of type T
    */
  def parseJson[T](source: Source)(implicit format: JsonFormat[T]): Stream[T] = {
    for (line <- source.getLines().toStream) yield line.parseJson.convertTo[T]
  }

  /** Writes the given objects to the given output stream, as one-per-line JSON values. */
  def writeJson[T](values: Iterable[T], outputStream: OutputStream)(
    implicit format: JsonFormat[T]): Unit = {
    Resource.using(new PrintWriter(outputStream)) { writer =>
      for (value <- values) {
        writer.println(value.toJson.compactPrint)
      }
    }
  }
}
