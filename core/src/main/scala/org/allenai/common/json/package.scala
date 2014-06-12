package org.allenai.common

import spray.json.DefaultJsonProtocol

/** Utilities for making it easier to work with spray-json for common use cases.
  *
  * Usage:
  * (format: OFF)
  * {{{
  * import spray.json._
  * import org.allenai.common.json._
  *
  * val json: JsObject = ...
  * val packed = json.pack("foo" -> 5) // creates a new JsObject with the added field
  * packed[Int]("foo") // returns 5
  * packed.get[Int]("foo") // returns Some(5)
  *
  * }}}
  * (format: ON)
  */
package object json extends DefaultJsonProtocol {

  import spray.json._

  implicit class RichJsObject(val jsObj: JsObject) extends AnyVal {

    /** Create a new JsObject with an additional field */
    def pack(newField: (String, JsValue)): JsObject = {
      JsObject(jsObj.fields + newField)
    }

    /** Create a new JsObject with an additional field */
    def pack[A : JsonWriter](newField: (String, A)): JsObject = {
      val aJsValue = implicitly[JsonWriter[A]].write(newField._2)
      pack(newField._1 -> aJsValue)
    }

    /** Extract a value of type A by the given key */
    def apply[A : JsonReader](key: String): A = jsObj.fields(key).convertTo[A]

    /** Extract a value of type A by the given key */
    def get[A : JsonReader](key: String): Option[A] = jsObj.fields.get(key) map (_.convertTo[A])
  }
}
