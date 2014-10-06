package org.allenai.common.json

import org.allenai.common.testkit.UnitSpec

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.{ Try, Success, Failure }

class PackedJsonFormatSpec extends UnitSpec {

  sealed trait Super
  case class NamedChild(name: String) extends Super
  case class NumberedChild(number: Int) extends Super

  object Super {
    // a PackedJsonFormat[NamedChild] that packs a field "type" -> JsString("named") during write
    implicit val namedFormat = jsonFormat1(NamedChild.apply).pack("type" -> "named")

    // a PackedJsonFormat[NumberedChild] that packs a field "type" -> JsString("numbered") during
    // write
    implicit val numberedFormat = jsonFormat1(NumberedChild.apply).pack("type" -> "numbered")

    implicit val unpackers = Seq(namedFormat, numberedFormat)

    implicit object SuperFormat extends RootJsonFormat[Super] {
      override def write(child: Super): JsValue = child match {
        case named: NamedChild => named.toJson
        case numbered: NumberedChild => numbered.toJson
      }

      override def read(jsValue: JsValue): Super = {
        // The `unpack` member of a PackedJsonFormat[T] is a PartialFunction[JsValue, T].
        // This is nice because it allows us to combine several `unpack` partial functions into
        // one using the `orElse` combinator. The result is the same as if you write out
        // case statements with guards for each of the `unpack` partial functions. For example,
        // you could write the following:
        //
        // (format: OFF)
        // jsValue match {
        //   case JsObject(fields) if fields.get("type") == Some("named") =>
        //     jsValue.convertTo[NamedChild]
        //   case JsObject(fields) if fields.get("type") == Some("numbered") =>
        //     jsValue.convertTo[NumberedChild]
        //   case _ => deserializationError("Missing valid `type` field")
        // }
        // (format: ON)
        //
        // However, the `unpack` partial functions already provide the guard (check for packed
        // field value), and with a helper method imported from org.allenai.common.json you can
        // simply do this:

        // using implicit Seq[PackedJsonFormat[_ <: Super]]:
        jsValue.asJsObject.unpackAs[Super]

        // or more explicitly:
        // jsValue.asJsObject.unpackWith[Super](namedFormat, numberedFormat)
      }
    }
  }

  "PackedJsonFormat" should "be useful for abstract class formats" in {
    val supers: Seq[Super] = Seq(NamedChild("foo"), NumberedChild(10)) // scalastyle:ignore
    val json = supers.toJson
    val fromJson = json.convertTo[Seq[Super]]
    assert(supers === fromJson)
  }
}
