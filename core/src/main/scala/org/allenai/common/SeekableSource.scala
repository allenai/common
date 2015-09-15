package org.allenai.common

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.NoSuchElementException

import scala.io.Codec

/** A class that provides seekability to an interface similar to scala.io.Source. This is required
  * because scala.io.Source only provides character offsets, while FileChannel (and other seekable
  * Java interfaces) take byte offsets for seeking. The only limitation is that the file must be
  * either UTF-8 or ISO-8859-1 encoded.
  * <p>This also has a `getLines()` method to iterate over lines. Any reads done on this iterator
  * are reflected in the main Source, and reads on the Source are reflected in the
  * `Iterator[String]`.</p>
  * @param inFile the file channel to wrap
  * @param bufferSize the size of the internal buffer to use
  * @param codec the codec to use. Must be one of UTF-8 or ISO-8859-1.
  * @throws IllegalArgumentException if `bufferSize` is less than 3, or `codec` is not UTF-8 or
  * ISO-8859-1
  */
class SeekableSource(inFile: FileChannel, bufferSize: Int = 8192)(implicit codec: Codec)
    extends Iterator[Char] {
  require(bufferSize >= 3, "Buffer must be at least 3 bytes to decode UTF-8!")

  // The motivation for this class is explained above, but some of the implementation choices are a
  // little confusing. This has decoding implemented within the class below, which seems odd. The
  // reason for this is that the Decoder interfaces in java.nio.charset don't provide feedback for
  // the number of bytes consumed while decoding a character, and trying to infer this (say, by
  // passing buffers of size 1, 2, 3, etc. until it successfully decodes the next character) are
  // incredibly inefficient.
  //
  // This uses manual decoding of individual characters for the Iterator[Char], but uses builtin
  // decoding for linewise reading for the Iterator[String] returned from getLines().

  /** True if we're decoding as UTF-8; false if we're decoding as ISO-8859-1. */
  val useUtf8 = if (codec.name == Codec.UTF8.name) {
    true
  } else if (codec.name == Codec.ISO8859.name) {
    false
  } else {
    throw new IllegalArgumentException(s"Unsupported codec ${codec.name}")
  }

  /** True if the inFile still has data to read. */
  private[common] var inputRemaining = true

  /** The buffer to read input into and out of. Initialized to empty. */
  private[common] var inBuffer = {
    val buffer = ByteBuffer.allocateDirect(bufferSize)
    buffer.limit(0)
    buffer
  }

  /** Returns true if the source has more input, populating the input buffer if need be. */
  override def hasNext: Boolean = inBuffer.hasRemaining || (inputRemaining && fillBuffer())

  /** Reads and returns a single character from a the file. */
  override def next(): Char = {
    if (inBuffer.hasRemaining || (inputRemaining && fillBuffer())) {
      if (useUtf8) {
        nextUtf8
      } else {
        nextIso8859
      }
    } else {
      throw new NoSuchElementException("next() called on empty SeekableSource")
    }
  }

  /** Positions this source at the given byte offset in the input file. Note that this will allow
    * you to position past the end of the file, in which case no input will be read.
    */
  def position(newPosition: Long): Unit = {
    inFile.position(newPosition)
    inBuffer.limit(0)
    inputRemaining = true
  }

  /** @return the current position in the file */
  def position: Long = inFile.position - inBuffer.remaining

  /** @return the next character in the buffer, decoded from ISO-8859-1 */
  protected[common] def nextIso8859: Char = {
    // `toChar` doesn't work as expected with bytes; they're first widened with sign extension,
    // meaning that bytes with the high bit set expand into two-byte characters with the high byte
    // set to 0xff. Mask into an int first to fix this.
    (inBuffer.get() & 0xff).toChar
  }

  /** @return the next character in the buffer, decoded from UTF-8 */
  protected[common] def nextUtf8: Char = {
    // Ensure we are either at the end of file, or have at least three bytes that are readable.
    if (inBuffer.remaining() < 3 && inputRemaining) {
      fillBuffer()
    }
    // See https://en.wikipedia.org/wiki/UTF-8 for details. This is checking the first byte for the
    // total length of the encoding.
    val first = inBuffer.get()
    val intVal = if ((first & 0x80) == 0) {
      return first.toChar
    } else if ((first & 0xe0) == 0xc0) {
      // TODO(jkinkead): Secondary bytes (here and below) should all have their two highest bits set
      // as 10. This should be asserted somehow, and if they're corrupted, producing an unknown
      // character instead.
      val second = inBuffer.get()
      ((first & 0x1f) << 6) | (second & 0x3f)
    } else if ((first & 0xf0) == 0xe0) {
      val second = inBuffer.get()
      val third = inBuffer.get()
      ((first & 0x0f) << 12) | ((second & 0x3f) << 6) | (third & 0x3f)
    } else {
      // Anything here is technically illegal UTF-8. However, some sources (i.e. Wikipedia) use
      // wide UTF-8 encodings. This will attempt to skip the correct number of bytes. Note that
      // while we could decode the character here, it will be wider than 16 bits, and therefore
      // won't fit in a JVM Char.

      // skipCount stores the *additional* bytes we will skip, not the total length of the encoding.
      val skipCount = if ((first & 0xf8) == 0xf0) {
        3
      } else if ((first & 0xfc) == 0xf8) {
        4
      } else if ((first & 0xfe) == 0xfc) {
        5
      } else {
        // This will be reached if the first character was truly illegal UTF-8, not just a wide
        // encoding.
        0
      }
      // Technically this is buggy if the buffer is 4 or 5 bytes - we won't skip the full character.
      // That's fine.
      if (inBuffer.remaining() < skipCount && inputRemaining) {
        fillBuffer()
      }
      (0 until skipCount) foreach { _ => if (inBuffer.hasRemaining()) inBuffer.get() }
      // Replace with the unknown character.
      0xfffd
    }
    intVal.toChar
  }

  /** Fills inBuffer from the input file. Sets `inputRemaining` as appropriate.
    * @return true if there are more characters to read in the buffer
    */
  protected[common] def fillBuffer(): Boolean = {
    inBuffer.compact()
    val numRead = inFile.read(inBuffer)
    inputRemaining = numRead != -1
    inBuffer.flip()

    inBuffer.hasRemaining
  }

  /** Returns an iterator over the remaining lines in this SeekableSource. Note that any reads done
    * on this iterator will be reflected in the main SeekableSource!
    * <p>This acts in the same way as java.io.BufferedReader or scala.io.Source: Single newlines or
    * single carriage returns are treated as an end-of-line, as are carriage returns followed
    * immediately by newlines.</p>
    */
  def getLines(): Iterator[String] = new LineIterator

  /** Iterator class for reading the file linewise. This keeps a byte array that's a mirror of the
    * ByteBuffer in the wrapping SeekableSource. This is scanned for encoded newlines and / or
    * carriage returns - which we can do because we only support two encodings, both of which use
    * the same single byte for these characters.
    */
  private[common] class LineIterator extends Iterator[String] {
    // Byte values of newlin & carriage return.
    val newline = '\n'.toByte
    val carriageReturn = '\r'.toByte
    val charset = codec.charSet

    /** The buffer to read the file into, and create strings out of. */
    private[common] val lineBuffer: Array[Byte] = new Array(bufferSize)
    /** The current index into lineBuffer. Kept in sync with inBuffer.position. */
    private[common] var index = 0
    /** The index of the last valid byte in lineBuffer. Kept in sync with inBuffer.limit. */
    private[common] var limit = 0

    /** Fills the line buffer if it needs filling.
      * @return true if there is still input available
      */
    private[common] def ensureFullBuffer(): Boolean = {
      // If we're at the end of our lineBuffer, we need to read more from SeekableSource.
      if (index == limit) {
        // Repopulate inBuffer via hasNext.
        if (SeekableSource.this.hasNext) {
          val capacity = Math.min(inBuffer.remaining, lineBuffer.length)
          val startPosition = inBuffer.position
          // Copy data to lineBuffer, and reset our indexes.
          inBuffer.get(lineBuffer, startPosition, capacity)
          index = startPosition
          limit = startPosition + capacity
          inBuffer.position(startPosition)
          true
        } else {
          false
        }
      } else {
        true
      }
    }

    override def hasNext: Boolean = ensureFullBuffer()

    /** Returns the next line in inBuffer. */
    override def next(): String = {
      if (!ensureFullBuffer()) {
        throw new NoSuchElementException("next() called with no lines remaining")
      }
      // Update our index + limit, in case the underlying SeekableSource has moved since we were last
      // iterated on.
      index = inBuffer.position
      limit = inBuffer.limit
      // Holds the string we're building the second and subsequent times through the loop below. If
      // we find a newline contained entirely in the buffer, we'll just create a string directly.
      var stringBuilder: StringBuilder = null
      while (ensureFullBuffer()) {
        var start = index

        // Read chars until we find a newline or the end-of-buffer.
        var ch: Byte = 0
        var foundEol = false
        while (index != limit && !foundEol) {
          ch = lineBuffer(index)
          index += 1
          if (ch == newline || ch == carriageReturn) {
            foundEol = true
          }
        }
        val length = index - start

        // Scan the parent's input buffer to the position we just read to.
        inBuffer.position(index)

        // We've reached the end of the buffer or the end of the line.
        if (foundEol) {
          // The string we've built. Remove the EOL.
          val str = new String(lineBuffer, start, length - 1, charset)
          // Skip a newline that follows a carriage return.
          if (ch == carriageReturn && ensureFullBuffer() && lineBuffer(index) == newline) {
            index += 1
            inBuffer.position(index)
          }
          return if (stringBuilder == null) str else stringBuilder.append(str).toString
        }

        if (stringBuilder == null) {
          stringBuilder = new StringBuilder(120)
        }
        stringBuilder.append(new String(lineBuffer, start, length, charset))
      }
      stringBuilder.toString
    }
  }
}
