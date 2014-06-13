package org.allenai.common.json

import org.allenai.common.testkit.UnitSpec

import spray.json._

import scala.util.{ Try, Success, Failure }

class PackedJsonFormatSpec extends UnitSpec {

  sealed trait Super
  case class NamedChild(name: String) extends Super
  case class NumberedChild(number: Int) extends Super

  object Super {
    // a PackedJsonFormat[NamedChild] that packs a field "type" -> JsString("named") during write
    implicit val namedFormat = jsonFormat1(NamedChild.apply).pack("type" -> "named")

    // a PackedJsonFormat[NumberedChild] that packs a field "type" -> JsString("numbered") during write
    implicit val numberedFormat = jsonFormat1(NumberedChild.apply).pack("type" -> "numbered")

    implicit object SuperFormat extends RootJsonFormat[Super] {
      override def write(child: Super): JsValue = child match {
        case named: NamedChild => named.toJson
        case numbered: NumberedChild => numbered.toJson
      }

      override def read(jsValue: JsValue): Super = {
        (namedFormat.unpack orElse numberedFormat.unpack).lift(jsValue) getOrElse {
          deserializationError("Missing valid `type` field")
        }
      }
    }
  }

  "PackedJsonFormat" should "be useful for abstract class formats" in {
    val supers: Seq[Super] = Seq(NamedChild("foo"), NumberedChild(10))
    val json = supers.toJson
    val fromJson = json.convertTo[Seq[Super]]
    assert(supers === fromJson)
  }
}
