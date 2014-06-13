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

  implicit class RichJsonFormat[T](val jsFormat: JsonFormat[T]) {

    /** Generate a PackedJsonFormat[T] that packs the packField into
      * the JsObject on write, and provides an `unpack` partial function
      * for handling reads.
      *
      * @param packField the key -> value pair to pack into the JsObject
      *        on write. Typically, this would be a type indicator.
      */
    def pack(packField: (String, JsValue)): PackedJsonFormat[T] =
      new PackedJsonFormat[T](jsFormat, packField)

    /** Generate a PackedJsonFormat[T] that packs the packField into
      * the JsObject on write, and provides an `unpack` partial function
      * for handling reads.
      *
      * @param packField the key -> value pair to pack into the JsObject
      *        on write. Typically, this would be a type indicator.
      */
    def pack[A : JsonWriter](packField: (String, A)): PackedJsonFormat[T] =
      pack(packField._1 -> packField._2.toJson)
  }
}
