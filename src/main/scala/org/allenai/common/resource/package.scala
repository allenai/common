package org.allenai.common

package object resource {
  // Allow use of methods on structural types.
  import scala.language.reflectiveCalls

  type Closeable = { def close(): Unit }

  /** A method for managing a simple resource.  After running a computation,
    * the resource will be closed and the computation will be returned.
    *
    * More complicated resource management may warrant a dependency on
    * "com.jsuereth" % "scala-arm", but this simple method handles most
    * cases.
    *
    * @param  resource  a closeable resource
    * @param  f  a computation involving the supplied resource
    * @returns  the result of the computation over the resource
    */
  def using[A <: Closeable, B](resource: => A)(f: A => B) {
    require(resource != null, "The supplied resource was null.")
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }
}
