package org.allenai.common

import scala.collection.Iterator
import scala.io.{Codec, Source}

import java.io.InputStream
import java.nio.{ByteBuffer, CharBuffer}

/** Input stream wrapping a Source object, using the codec to convert characters to bytes. Not
  * thread-safe.
  */
class SourceInputStream(val source: Source)(implicit codec: Codec) extends InputStream {

  /** Buffer to write (potentially multi-byte) character encodings to. */
  private val outputBuffer = ByteBuffer.allocate(codec.encoder.maxBytesPerChar.ceil.toInt)

  /** Number of bytes left in our output buffer. */
  private var availableBytes = 0

  /** Buffer to re-use when passing characters to our encoder. */
  private val charBuffer = Array[Char](1)

  override def read: Int = {
    // If we have no available bytes read, but we have characters in our Source, read the next
    // character into our byte array.
    if (availableBytes <= 0 && source.hasNext) {
      readNextChar()
    }
    // At this point, if we have no bytes, we are at the end of our stream.
    if (availableBytes <= 0) {
      -1
    } else {
      availableBytes -= 1
      outputBuffer.get()
    }
  }

  /** Reads the next character from the underlying source, encodes it into `outputBuffer`, and sets
    * `availableBytes` to the number of bytes written.
    */
  private def readNextChar(): Unit = {
    // Reset the buffer before writing.
    outputBuffer.rewind()

    // Read & encode the result.
    charBuffer(0) = source.next()
    val result = codec.encoder.encode(CharBuffer.wrap(charBuffer), outputBuffer, false)
    if (result.isOverflow) {
      // Shouldn't happen unless there's a bug in the codec (the output buffer should always have
      // enough room).
      result.throwException()
    }

    // Set the availble bytes & reset the buffer for read.
    availableBytes = outputBuffer.position
    outputBuffer.rewind()
  }
}
