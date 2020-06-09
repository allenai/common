package org.allenai.common

import org.allenai.common.testkit.UnitSpec

import spray.json.DefaultJsonProtocol._

import scala.io.Source

import java.io.ByteArrayOutputStream

class JsonIoSpec extends UnitSpec {
  case class Foo(name: String)
  implicit val fooFormat = jsonFormat1(Foo.apply)

  "parseJson" should "read a multi-line string as two instances" in {
    val input = s"""{"name": "value1"}\n{"name": "value2"}\n"""
    val result = JsonIo.parseJson[Foo](Source.fromString(input))(fooFormat).toSeq
    result should have size (2)
    result(0) should be(Foo("value1"))
    result(1) should be(Foo("value2"))
  }

  "writeJson" should "write instances correctly" in {
    val input = Seq(Foo("first"), Foo("second"), Foo("third"))
    val output = new ByteArrayOutputStream()
    JsonIo.writeJson(input, output)

    val outputStrings = output.toString("UTF-8").split("\n")
    outputStrings should have size (3)
    outputStrings(0) should fullyMatch regex ("""\{\s*"name":\s*"first"\s*\}""")
    outputStrings(1) should fullyMatch regex ("""\{\s*"name":\s*"second"\s*\}""")
    outputStrings(2) should fullyMatch regex ("""\{\s*"name":\s*"third"\s*\}""")
  }

  "parseJson and writeJson" should "pipe correctly to each other" in {
    // Input. We'll pipe through writeJson & toJson twice (testing both directions).
    val input = List(Foo("a"), Foo("b"), Foo("c"), Foo("d"))

    // Intermediary: Test that write -> read works.
    val buffer = new ByteArrayOutputStream()
    JsonIo.writeJson(input, buffer)
    val intermediaryOutput = JsonIo.parseJson[Foo](Source.fromString(buffer.toString("UTF8")))
    intermediaryOutput.toList should be(input)

    // Final: Test that read -> write works (with a bonus read).
    buffer.reset()
    JsonIo.writeJson(input, buffer)
    val finalOutput = JsonIo.parseJson[Foo](Source.fromString(buffer.toString("UTF8")))
    finalOutput.toList should be(input)
  }
}
