package org.allenai.common

import org.allenai.common.JsonFormats._
import org.allenai.common.testkit.UnitSpec

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.{ Try, Success, Failure }

class JsonFormatsSpec extends UnitSpec {

  case class Foo(name: String)
  implicit val fooFormat = jsonFormat1(Foo.apply)

  "ThrowableWriter" should "write message and stackTrace" in {
    val e = new Exception("my message")
    //e.printStackTrace
    val json = e.toJson
    val jsonObj = json.asJsObject
    assert(jsonObj.fields("message") === JsString("my message"))
    assert(jsonObj.fields("stackTrace") !== JsString("()"))
  }

  "TryWriter" should "write success" in {
    val success: Try[Foo] = Success(Foo("foo"))
    val js = success.toJson
    assert(js === JsObject("success" -> JsObject("name" -> JsString("foo"))))
  }

  it should "write failure" in {
    val failure: Try[Foo] = Failure(new IllegalArgumentException("bar"))
    val js = failure.toJson
    val failureJs = js.asJsObject.fields("failure").asJsObject
    assert(failureJs.fields("message") === JsString("bar"))
  }
}
