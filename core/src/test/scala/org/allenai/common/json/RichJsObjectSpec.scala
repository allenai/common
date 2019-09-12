package org.allenai.common.json

import org.allenai.common.testkit.UnitSpec

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.{ Failure, Success, Try }

// scalastyle:off magic.number
class RichJsObjectSpec extends UnitSpec {

  case class Foo(name: String)
  implicit val fooFormat = jsonFormat1(Foo.apply)

  "pack" should "return a new JsObject with an additional field" in {
    val foo = Foo("John")
    val json = foo.toJson
    val jsonObj = json.asJsObject
    val packed = jsonObj.pack("age" -> 10.toJson)
    assert(
      packed === JsObject(
        "name" -> JsString("John"),
        "age" -> JsNumber(10)
      )
    )
  }

  it should "handle types that are not JsValue but have a JsonWriter" in {
    val foo = Foo("John")
    val json = foo.toJson
    val jsonObj = json.asJsObject
    val packed = jsonObj.pack("age" -> 10)
    assert(
      packed === JsObject(
        "name" -> JsString("John"),
        "age" -> JsNumber(10)
      )
    )
  }
}
