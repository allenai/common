package org.allenai.common

import org.allenai.common.testkit.UnitSpec

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{ Files, StandardOpenOption }

import scala.io.Codec

class SeekableSourceSpec extends UnitSpec {
  // UTF-8 is the default encoding for these tests.
  implicit val codec = Codec.UTF8

  /** Stores fü入, in UTF-8. */
  val foomlaut: Array[Byte] = Array('f'.toByte, 0xc3, 0xbc, 0xe5, 0x85, 0xa5) map { _.toByte }

  /** @return a channel open to a new temp file containing the given chars */
  def newFileWithChars(chars: Iterable[Char]): FileChannel = {
    newFileWithBytes(chars.toArray map { _.toByte })
  }

  /** @return a channel open to a new temp file containing the given bytes */
  def newFileWithBytes(bytes: Array[Byte]): FileChannel = {
    val path = Files.createTempFile(null, null)
    val channel = FileChannel.open(
      path,
      StandardOpenOption.DELETE_ON_CLOSE,
      StandardOpenOption.READ,
      StandardOpenOption.WRITE
    )
    val buffer = ByteBuffer.allocate(bytes.length)
    buffer.put(bytes)
    buffer.rewind
    channel.write(buffer)
    channel.position(0)
    channel
  }

  "SeekableSource" should "read a simple file in UT8" in {
    val foo = newFileWithChars("foo")

    val source = new SeekableSource(foo)
    source.mkString should be("foo")
  }

  it should "read a simple file in ISO-8859-1" in {
    val foo = newFileWithChars("foo")

    val source = new SeekableSource(foo)(Codec.ISO8859)
    source.mkString should be("foo")
  }

  it should "read a file in UTF-8 with a small buffer that crosses character boundaries" in {
    val testfile = newFileWithBytes(foomlaut)

    // This buffer size will cross character boundaries (the first read will hit the middle of the
    // 入).
    val source = new SeekableSource(testfile, bufferSize = 4)
    source.mkString should be("fü入")
  }

  it should "read lines in UTF-8 with a small buffer that crosses character boundaries" in {
    var testfile = newFileWithBytes(
      (foomlaut :+ '\n'.toByte) ++ ('&'.toByte +: foomlaut :+ '\n'.toByte)
    )
    var source = new SeekableSource(testfile, bufferSize = 4)
    source.getLines().mkString should be("fü入&fü入")

    testfile = newFileWithBytes(
      (foomlaut :+ '\n'.toByte) ++ ('&'.toByte +: foomlaut :+ '\n'.toByte)
    )
    source = new SeekableSource(testfile, bufferSize = 5)
    withClue("Reading to the middle of a three-byte char") {
      source.getLines().mkString should be("fü入&fü入")
    }

    testfile = newFileWithBytes(
      (foomlaut :+ '\n'.toByte) ++ ('&'.toByte +: foomlaut :+ '\n'.toByte)
    )
    source = new SeekableSource(testfile, bufferSize = 6)
    withClue("Reading to the end of a three-byte char") {
      source.getLines().mkString should be("fü入&fü入")
    }
    testfile = newFileWithBytes(
      (foomlaut :+ '\n'.toByte) ++ ('&'.toByte +: foomlaut :+ '\n'.toByte)
    )
    source = new SeekableSource(testfile, bufferSize = 7)
    source.getLines().mkString should be("fü入&fü入")
    testfile = newFileWithBytes(
      (foomlaut :+ '\n'.toByte) ++ ('&'.toByte +: foomlaut :+ '\n'.toByte)
    )
    source = new SeekableSource(testfile, bufferSize = 8)
    source.getLines().mkString should be("fü入&fü入")
    testfile = newFileWithBytes(
      (foomlaut :+ '\n'.toByte) ++ ('&'.toByte +: foomlaut :+ '\n'.toByte)
    )
    source = new SeekableSource(testfile, bufferSize = 9)
    source.getLines().mkString should be("fü入&fü入")
    testfile = newFileWithBytes(
      (foomlaut :+ '\n'.toByte) ++ ('&'.toByte +: foomlaut :+ '\n'.toByte)
    )
    source = new SeekableSource(testfile, bufferSize = 10)
    source.getLines().mkString should be("fü入&fü入")
  }

  it should "report the correct byte position when reading wide chars" in {
    val testfile = newFileWithBytes(foomlaut)
    val source = new SeekableSource(testfile)

    withClue("Start position: ") { source.position should be(0) }
    source.next()
    withClue("After reading single-byte char: ") { source.position should be(1) }
    source.next()
    withClue("After reading two-byte char: ") { source.position should be(3) }
    source.next()
    withClue("After reading three-byte char: ") { source.position should be(6) }
  }

  it should "allow positioning in a file" in {
    val testfile = newFileWithBytes(foomlaut)
    val source = new SeekableSource(testfile)

    source.position(3)
    withClue("After scanning to final character: ") { source.next() should be('入') }
    withClue("After reading the final character: ") {
      source.position should be(6)
      source.hasNext should be(false)
    }

    source.position(0)
    withClue("After reading and resetting: ") { source.mkString("") should be("fü入") }
  }

  it should "read high-valued ISO-8859-1 characters" in {
    // Mu, encoded in ISO-8859-1.
    val catsSay = newFileWithBytes(Array(0xb5.toByte))
    val source = new SeekableSource(catsSay)(Codec.ISO8859)

    source.mkString("") should be("µ")
  }

  "SeekableSource.getLines" should "read lines from a file" in {
    val newlines = newFileWithChars("a\nb")
    val source = new SeekableSource(newlines)
    val lines = source.getLines()
    withClue("The first line: ") { lines.next() should be("a") }
    withClue("Position after reading the first line: ") { source.position should be(2) }
    withClue("The second line: ") { lines.next() should be("b") }
    withClue("Position after reading the second line: ") { source.position should be(3) }
    lines.hasNext should be(false)
  }

  it should "handle lines that span buffer boundaries" in {
    val newlines = newFileWithChars("abcde\nfghij\n")
    val source = new SeekableSource(newlines, 4)
    val lines = source.getLines()

    lines.next() should be("abcde")
    withClue("Position after reading the first line: ") { source.position should be(6) }
    lines.next() should be("fghij")
    withClue("Position after reading the second line: ") { source.position should be(12) }
    lines.hasNext should be(false)
  }

  it should "handle empty lines" in {
    val newlines = newFileWithChars("\n\na\n\n")
    val source = new SeekableSource(newlines)
    val lines = source.getLines()

    lines.next() should be("")
    lines.next() should be("")
    lines.next() should be("a")
    lines.next() should be("")
    lines.hasNext should be(false)
  }

  it should "handle mixing getLines with getting chars" in {
    val newlines = newFileWithChars("foo\nbar")
    val source = new SeekableSource(newlines)
    val lines = source.getLines()

    source.next() should be('f')
    lines.next() should be("oo")
    source.next() should be('b')
    lines.next() should be("ar")
  }

  it should "handle carriage returns correctly" in {
    val windowsNewlines = newFileWithChars("foo\rbar\r\ngaz\r")
    val source = new SeekableSource(windowsNewlines)
    val lines = source.getLines()

    lines.next() should be("foo")
    lines.next() should be("bar")
    lines.next() should be("gaz")
    lines.hasNext should be(false)
  }

  it should "handle four-byte unicode characters" in {
    val thumbsUp = newFileWithBytes(Array('u', 0xf0, 0x9f, 0x91, 0x8d, 'p') map { _.toByte })
    val source = new SeekableSource(thumbsUp)

    source.next() should be('u')
    withClue("the first character decoded:") {
      source.next().toInt.toBinaryString should be('\ud83d'.toInt.toBinaryString)
    }
    source.position should be(3)
    withClue("the second character decoded:") {
      source.next().toInt.toBinaryString should be('\udc4d'.toInt.toBinaryString)
    }
    source.position should be(5)
    source.next() should be('p')
  }

  it should "handle malformed input correctly" in {
    // Valid letter, invalid start, bad three-byte char, valid letter.
    val badChars = newFileWithBytes(Array('a', 0xff, 0xe0, 0x03, 0x8f, 'b') map { _.toByte })
    val source = new SeekableSource(badChars)

    source.next() should be('a')
    source.next() should be('\ufffd')
    source.position should be(2)
    source.next() should be('\ufffd')
    source.position should be(5)
    source.next() should be('b')
  }

  it should "handle partial two-byte characters at the end of a stream" in {
    val earlyEnd = newFileWithBytes(Array('a', 0xc3) map { _.toByte })
    val source = new SeekableSource(earlyEnd)

    source.next() should be('a')
    source.next() should be('\ufffd')
  }

  it should "handle partial three-byte characters at the end of a stream" in {
    val earlyEnd = newFileWithBytes(Array('a', 0xe5, 0x85) map { _.toByte })
    val source = new SeekableSource(earlyEnd)

    source.next() should be('a')
    source.next() should be('\ufffd')
  }

  it should "handle partial four-byte characters at the end of a stream" in {
    val earlyEnd = newFileWithBytes(Array('a', 0xf0, 0x9f, 0x91) map { _.toByte })
    val source = new SeekableSource(earlyEnd)

    source.next() should be('a')
    source.next() should be('\ufffd')
  }
}
