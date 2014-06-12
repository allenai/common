package org.allenai.common.json

import org.allenai.common.testkit.UnitSpec

import spray.json._

import scala.util.{ Try, Success, Failure }

class RichJsObjectSpec extends UnitSpec {

  case class Foo(name: String)
  implicit val fooFormat = jsonFormat1(Foo.apply)

  sealed trait Super
  case class NamedChild(name: String) extends Super
  case class NumberedChild(number: Int) extends Super
  object Super {
    implicit val namedFormat = jsonFormat1(NamedChild.apply)
    implicit val numberedFormat = jsonFormat1(NumberedChild.apply)
    implicit object SuperFormat extends RootJsonFormat[Super] {
      override def write(child: Super): JsValue = child match {
        case named: NamedChild => named.toJson.asJsObject.pack("type" -> "named")
        case numbered: NumberedChild => numbered.toJson.asJsObject.pack("type" -> "numbered")
      }

      override def read(jsValue: JsValue): Super = jsValue.asJsObject.apply[String]("type") match {
        case "named" => jsValue.convertTo[NamedChild]
        case "numbered" => jsValue.convertTo[NumberedChild]
      }
    }
  }

  "pack" should "return a new JsObject with an additional field" in {
    val foo = Foo("John")
    val json = foo.toJson
    val jsonObj = json.asJsObject
    val packed = jsonObj.pack("age" -> 10.toJson)
    assert(packed === JsObject(
      "name" -> JsString("John"),
      "age" -> JsNumber(10)))
  }

  it should "handle types that are not JsValue but have a JsonWriter" in {
    val foo = Foo("John")
    val json = foo.toJson
    val jsonObj = json.asJsObject
    val packed = jsonObj.pack("age" -> 10)
    assert(packed === JsObject(
      "name" -> JsString("John"),
      "age" -> JsNumber(10)))
  }

  it should "be useful for abstract class formats" in {
    val supers: Seq[Super] = Seq(NamedChild("foo"), NumberedChild(10))
    val json = supers.toJson
    val fromJson = json.convertTo[Seq[Super]]
    assert(supers === fromJson)
  }
}
