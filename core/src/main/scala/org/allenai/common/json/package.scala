package org.allenai.common

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
package object json {

  import spray.json._

  implicit class RichJsObject(val jsObj: JsObject) extends AnyVal {

    /** Create a new JsObject with an additional field */
    def pack(newField: (String, JsValue)): JsObject = {
      JsObject(jsObj.fields + newField)
    }

    /** Create a new JsObject with an additional field */
    def pack[A: JsonWriter](newField: (String, A)): JsObject = {
      val aJsValue = implicitly[JsonWriter[A]].write(newField._2)
      pack(newField._1 -> aJsValue)
    }

    /** Optionally unpack json using the provided PackedJsonFormats
      *
      * @param packedFormats
      */
    def unpackOptWith[T](packedFormats: PackedJsonFormat[_ <: T]*): Option[T] = {
      val unpacks: Seq[PartialFunction[JsValue, T]] = packedFormats
        .map(_.asInstanceOf[PackedJsonFormat[T]])
        .map(_.unpack)
      val combinedUnpack = unpacks reduce (_ orElse _)
      combinedUnpack.lift(jsObj)
    }

    /** Optionally unpack json using the provided PackedJsonFormats
      *
      * @param packedFormats
      * @throws spray.json.DeserializationException
      */
    def unpackWith[T](packedFormats: PackedJsonFormat[_ <: T]*): T = {
      unpackOptWith[T](packedFormats: _*) getOrElse {
        deserializationError(
          s"Invalid JSON. Expected a JsObject with a valid packed field, but got ${jsObj.toString}"
        )
      }
    }

    /** Unpack json using the implicitly provided PackedJsonFormats
      *
      * @tparam the type to unpack
      * @param packedFormats
      * @throws spray.json.DeserializationException
      */
    def unpackAs[T](implicit unpackers: Seq[PackedJsonFormat[_ <: T]]): T = {
      unpackOptWith[T](unpackers: _*) getOrElse {
        deserializationError(
          s"Invalid JSON. Expected a JsObject with a valid packed field, but got ${jsObj.toString}"
        )
      }
    }

    /** Extract a value of type A by the given key */
    def apply[A: JsonReader](key: String): A = jsObj.fields(key).convertTo[A]

    /** Extract a value of type A by the given key */
    def get[A: JsonReader](key: String): Option[A] = jsObj.fields.get(key) map (_.convertTo[A])
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
    def pack[A: JsonWriter](packField: (String, A)): PackedJsonFormat[T] =
      pack(packField._1 -> packField._2.toJson)
  }
}
