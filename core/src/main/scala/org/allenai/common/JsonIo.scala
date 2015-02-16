package org.allenai.common

import spray.json._

import scala.io.Source

import java.io.{ OutputStream, PrintWriter, Writer }

/** Helpers for streaming lists of JSON objects to and from disk. */
object JsonIo {
  /** Reads single-lines from a given Source, and streams the JSON parsed from them to the caller.
    * @return a stream of objects of type T
    */
  def parseJson[T](source: Source)(implicit format: JsonFormat[T]): Stream[T] = {
    for (line <- source.getLines().toStream) yield line.parseJson.convertTo[T]
  }

  /** Writes the given objects to the given writer, as one-per-line JSON values. */
  def writeJson[T](values: Iterable[T], writer: Writer)(implicit format: JsonFormat[T]): Unit = {
    for (value <- values) {
      writer.write(value.toJson.compactPrint)
      writer.write('\n')
    }
  }

  /** Writes the given objects to the given output stream, as one-per-line JSON values. */
  def writeJson[T](
    values: Iterable[T],
    outputStream: OutputStream
  )(implicit format: JsonFormat[T]): Unit = {
    val writer = new PrintWriter(outputStream)
    writeJson(values, writer)
    writer.flush
  }
}
