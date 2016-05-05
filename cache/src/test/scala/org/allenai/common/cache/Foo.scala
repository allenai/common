package org.allenai.common.cache

import spray.json.DefaultJsonProtocol._

/** Simple class for testing the cache can handle object structures. */
case class Foo(stringVar: String, intVar: Int)

object Foo {
  implicit val fooFormat = jsonFormat2(Foo.apply)
}
