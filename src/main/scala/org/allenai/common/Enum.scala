package org.allenai.common

import spray.json._
import spray.json.DefaultJsonProtocol._

/** Enumeration implementation that supports automatic Spray JSON serialization as JsString(id).
  *
  * @param id  value used for lookup and JSON serialization
  *
  * Usage:
  * {{{
  * sealed abstract class MyEnum(id: String) extends Enum[MyEnum](id)
  * object MyEnum extends EnumCompanion[MyEnum] {
  *   case object One extends MyEnum("one")
  *   case object Two extends MyEnum("two")
  *   register(One, Two)
  * }
  *
  * // JSON serialization:
  * MyEnum.One.toJson // JsString("one")
  * MyEnum.Two.toJson // JsString("two")
  * JsString("one").convertTo[MyEnum] // MyEnum.One
  * JsString("two").convertTo[MyEnum] // MyEnum.Two
  * }}}
  *
  */
abstract class Enum[E <: Enum[E]](val id: String)

/** Superclass for Enum companion objects providing enum registration and JSON serialization */
abstract class EnumCompanion[E <: Enum[E]] {

  /** Internal registry of enums */
  private[this] var registry = Map[String, E]()

  /** Lookup enum by ID
    * @param id
    * @throws
    */
  def withId(id: String): E = registry(id)

  def all: Iterable[E] = registry.values

  /** Register enums so they can be looked up by ID and be included in `all` iterable
    * @param enums
    */
  // TODO(markschaake): this might be a prime candidate for a macro which can generate
  // exhaustive pattern matching instead of realying on the user to manually register
  // each case object.
  protected def register(enums: E*) = enums foreach { e =>
    registry = registry + (e.id -> e)
  }

  implicit object EnumJsonFormat extends JsonFormat[E] {
    override def read(jsValue: JsValue): E = jsValue match {
      case JsString(id) => withId(id)
      case other => deserializationError(s"Enum id must be a JsString: $other")
    }
    override def write(e: E): JsValue = JsString(e.id)
  }
}
