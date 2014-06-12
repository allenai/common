package org.allenai.common

import org.allenai.common.testkit.UnitSpec

import scala.collection.mutable
import scala.io.{ Codec, Source }

class SourceInputStreamSpec extends UnitSpec {
  "SourceInputStream" should "handle ASCII (single-byte) characters correctly" in {
    val asciiString = "foobar"
    val stream = new SourceInputStream(Source.fromString(asciiString))(Codec.UTF8)
    val bytesRead = new mutable.ArrayBuffer[Byte]()
    var nextByte = stream.read()
    while (nextByte != -1) {
      bytesRead += nextByte.toByte
      nextByte = stream.read()
    }

    assert(bytesRead.toArray === Codec.toUTF8(asciiString))
  }

  it should "handle multi-byte characters correctly" in {
    val multibyteString = "ಠ_ಠ"
    val stream = new SourceInputStream(Source.fromString(multibyteString))(Codec.UTF8)
    val bytesRead = new mutable.ArrayBuffer[Byte]()
    var nextByte = stream.read()
    while (nextByte != -1) {
      bytesRead += nextByte.toByte
      nextByte = stream.read()
    }

    assert(bytesRead.toArray === Codec.toUTF8(multibyteString))
  }
}
