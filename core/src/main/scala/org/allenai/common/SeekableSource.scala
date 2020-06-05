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
  * `Iterator[String]`. The iterator returned by `getLines()` will create another buffer of
  * `bufferSize` bytes, so bear this in mind when estimating memory usage.</p>
  * @param inFile the file channel to wrap
  * @param bufferSize the size of the internal buffer to use. Defaults to 8MB.
  * @param codec the codec to use. Must be one of UTF-8 or ISO-8859-1.
  * @throws java.lang.IllegalArgumentException if `bufferSize` is less than 4, or `codec` is not
  * UTF-8 or ISO-8859-1
  */
class SeekableSource(inFile: FileChannel, bufferSize: Int = 8 << 20)(implicit codec: Codec)
    extends Iterator[Char] {
  require(bufferSize >= 4, "Buffer must be at least 4 bytes to decode UTF-8!")

  // "Unknown" character. Used as a replacement for bad characters encountered.
  val BadChar = 0xfffd

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

  /** True if we're in the middle of a two-Java-char unicode character. */
  private[common] var wideCharBytesRemaining = false

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
    // Fill the buffer if we need more bytes.
    if (inBuffer.remaining < 4 && inputRemaining) {
      fillBuffer()
    }
    // See https://en.wikipedia.org/wiki/UTF-8 for details. This is checking the first byte for the
    // total length of the encoding.
    val first = inBuffer.get()
    val intVal = if ((first & 0x80) == 0) {
      return first.toChar
    } else if ((first & 0xe0) == 0xc0) {
      if (inBuffer.hasRemaining) {
        // First byte starts with 110 - two-byte encoding.
        val second = inBuffer.get()
        // Verify this starts with 10.
        if ((second & 0xc0) == 0x80) {
          ((first & 0x1f) << 6) | (second & 0x3f)
        } else {
          BadChar
        }
      } else {
        BadChar
      }
    } else if ((first & 0xf0) == 0xe0) {
      if (inBuffer.remaining > 1) {
        // First byte starts with 1110 - three-byte encoding.
        val second = inBuffer.get()
        val third = inBuffer.get()
        // Verify these start with 10.
        if ((second & 0xc0) == 0x80 && (third & 0xc0) == 0x80) {
          ((first & 0x0f) << 12) | ((second & 0x3f) << 6) | (third & 0x3f)
        } else {
          BadChar
        }
      } else {
        while (inBuffer.hasRemaining) { inBuffer.get() }
        BadChar
      }
    } else if ((first & 0xf8) == 0xf0) {
      if (inBuffer.remaining > 2) {
        // First byte starts with 1111 - four-byte encoding. This needs to be split into two JVM
        // chars (encoded in UTF-16).
        val second = inBuffer.get()
        val third = inBuffer.get()
        val fourth = inBuffer.get()
        // Verify these start with 10.
        if ((second & 0xc0) == 0x80 && (third & 0xc0) == 0x80 && (fourth & 0xc0) == 0x80) {
          // Set up our next read. We need to push back the third  & fourth byte.
          wideCharBytesRemaining = true
          inBuffer.position(inBuffer.position - 2)

          // Decode the 10 bits we'll be encoding in the first UTF-16 char.  The 21st UTF-8 bit
          // (0x08 in the first byte) is discarded. UTF-8 four-byte encoding supports 21 bits.
          val rawBits = ((first & 0x03) << 8) | ((second & 0x3f) << 2) |
            ((third & 0x30) >> 4)

          // The result is 0xd800 + (these bits - 0x40).
          (0xd800 + rawBits - 0x40)
        } else {
          BadChar
        }
      } else {
        while (inBuffer.hasRemaining) { inBuffer.get() }
        BadChar
      }
    } else if (wideCharBytesRemaining) {
      wideCharBytesRemaining = false

      if (inBuffer.hasRemaining) {
        // First byte is the third of a UTF-8 four-byte encoding; second byte is the fourth byte in
        // a UTF-8 encoding.
        val second = inBuffer.get()
        // Verify the second byte starts with 10 (we already verified the first).
        if ((second & 0xc0) == 0x80) {
          (0xdc00) | ((first & 0x0f) << 6) | (second & 0x3f)
        } else {
          BadChar
        }
      } else {
        BadChar
      }
    } else {
      // Anything here is illegal UTF-8.

      // Skip any secondary UTF bytes (bytes starting with 10) in case we're seeing some very-wide
      // characters.
      var skip = true
      while (skip && (inBuffer.hasRemaining || fillBuffer())) {
        val currByte = inBuffer.get()
        if ((currByte & 0xc0) != 0x80) {
          // Not a secondary byte; move buffer back.
          skip = false
          inBuffer.position(inBuffer.position - 1)
        }
      }
      BadChar
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
        fillBuffer()
      } else {
        true
      }
    }

    /** Fills the buffer unconditionally.
      * @return true if there is still input available
      */
    private[common] def fillBuffer(): Boolean = {
      if (SeekableSource.this.fillBuffer()) {
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
    }

    override def hasNext: Boolean = ensureFullBuffer()

    /** Returns the next line in inBuffer. */
    override def next(): String = {
      if (!ensureFullBuffer()) {
        throw new NoSuchElementException("next() called with no lines remaining")
      }
      // Update our index + limit, in case the underlying SeekableSource has moved since we were
      // last iterated on.
      index = inBuffer.position
      limit = inBuffer.limit
      // Holds the string we're building the second and subsequent times through the loop below. If
      // we find a newline contained entirely in the buffer, we'll just create a string directly.
      var stringBuilder: StringBuilder = null
      var moreChars = true
      while (moreChars) {
        val start = index

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
        var length = index - start

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
        // Scan back from the end of the buffer to the start of any UTF-8 char we're encoding.
        // Check for a UTF-8 non-singular char (high bit set)
        if (useUtf8 && (ch & 0x80) != 0 && SeekableSource.this.inputRemaining) {
          val offset = if ((ch & 0xc0) == 0xc0) {
            // Start of a UTF-8 character. Skip back one.
            1
          } else {
            // Middle of a UTF-8 character. We have to figure out the start.
            var skipCount = 0
            while ((ch & 0xc0) == 0x80) {
              skipCount += 1
              ch = lineBuffer(index - skipCount - 1)
            }
            // Check if we have a full char; if so, don't skip.
            if ((ch & 0xe0) == 0xc0 && skipCount == 1) {
              0
            } else if ((ch & 0xf0) == 0xe0 && skipCount == 2) {
              0
            } else {
              skipCount + 1
            }
          }
          index -= offset
          inBuffer.position(index)
          length = index - start
        }
        stringBuilder.append(new String(lineBuffer, start, length, charset))
        moreChars = fillBuffer()
      }
      stringBuilder.toString
    }
  }
}
