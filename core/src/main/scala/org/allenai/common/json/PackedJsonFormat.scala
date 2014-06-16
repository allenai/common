package org.allenai.common.json

import spray.json._

/** Provides a Partial function that only deserializes if the packedField is present in the JSON
  * Because it is a PartialFunction, it can be easily combined with others in a series
  * of `orElse` calls. This makes the `unpack` especially useful for handling abstract
  * class deserialization where you have `packed` a type identifier into the JSON on
  * write. See the PackedJsonFormatSpec for an example.
  */
private[json] class PackedJsonFormat[T](jsFormat: JsonFormat[T], packField: (String, JsValue))
    extends RootJsonFormat[T] {

  /** Partial function that only deserializes if the packedField is present in the JSON */
  val unpack: PartialFunction[JsValue, T] = {
    case jsObject: JsObject if jsObject.fields.get(packField._1) == Some(packField._2) =>
      jsFormat.read(jsObject)
  }

  override def read(jsValue: JsValue): T = jsFormat.read(jsValue)
  override def write(t: T): JsValue = jsFormat.write(t).asJsObject.pack(packField)
}
