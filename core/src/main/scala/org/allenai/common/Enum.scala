package org.allenai.common

import spray.json.{deserializationError, JsString, JsValue, RootJsonFormat}

/** Enumeration implementation that supports automatic Spray JSON serialization of a case object as
  * a JsString, or using java native serialization for Spark jobs.
  *
  * Usage:
  * (format: OFF)
  * {{{
  * sealed abstract class MyEnum extends Enum[MyEnum]
  * object MyEnum extends EnumCompanion[MyEnum] {
  *   case object One extends MyEnum
  *   case object Two extends MyEnum
  *   register(One, Two)
  * }
  *
  * // JSON serialization:
  * MyEnum.One.toJson // JsString("One")
  * MyEnum.Two.toJson // JsString("Two")
  * JsString("One").convertTo[MyEnum] // MyEnum.One
  * JsString("Two").convertTo[MyEnum] // MyEnum.Two
  * }}}
  * (format: ON)
  */
abstract class Enum[E <: Enum[E]] extends Serializable {

  /** The serialization string. By default, use the toString implementation. For a case object, this
    * uses the object name.
    */
  def id: String = toString
}

/** Superclass for Enum companion objects providing enum registration and JSON serialization */
abstract class EnumCompanion[E <: Enum[E]] {

  /** Internal registry of enums */
  private[this] var registry = Map[String, E]()

  /** Lookup enum by ID
    * @param id
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

  implicit object EnumJsonFormat extends RootJsonFormat[E] {
    override def read(jsValue: JsValue): E = jsValue match {
      case JsString(id) => withId(id)
      case other => deserializationError(s"Enum id must be a JsString: $other")
    }
    override def write(e: E): JsValue = JsString(e.id)
  }
}
