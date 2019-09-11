package org.allenai.common

import java.net.URL

object Resource {
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
    * @return  the result of the computation over the resource
    */
  def using[A <: Closeable, B](resource: A)(f: A => B): B = {
    require(resource != null, "The supplied resource was null.")
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }

  def using2[A1 <: Closeable, A2 <: Closeable, B](
      resource1: A1,
      resource2: A2
  )(f: (A1, A2) => B): B = {
    require(resource1 != null, "The supplied resource was null.")
    require(resource2 != null, "The supplied resource was null.")
    try {
      f(resource1, resource2)
    } finally {
      resource1.close()
      resource2.close()
    }
  }

  /** Get a Java Resource.
    * This method provides a much nicer exception than the Java default (NPE).
    *
    * @param  name  absolute path of the resource
    */
  def getAbsolute(name: String): URL = {
    getOptAbsolute(name).getOrElse {
      throw new IllegalArgumentException("No such absolute resource found: " + name)
    }
  }

  /** Get a Java Resource, returning None if it could not be found.
    *
    * @param  name  absolute path of the resource
    * @returns  Some(URL) if the resource exists
    */
  def getOptAbsolute(name: String): Option[URL] = {
    Option(this.getClass.getClassLoader.getResource(name))
  }

  /** Get a Java Resource.
    * This method provides a much nicer exception than the Java default (NPE).
    *
    * @param  clazz  the class from which the resource may be relative
    * @param  name  path of the resource (absolute if prefixed with /)
    */
  def get(clazz: Class[_], name: String): URL = {
    getOpt(clazz, name).getOrElse {
      throw new IllegalArgumentException("No such absolute resource found: " + name)
    }
  }

  /** Get a Java Resource, returning None if it could not be found.
    *
    * @param  clazz  the class from which the resource may be relative
    * @returns  Some(URL) if the resource exists
    */
  def getOpt(clazz: Class[_], name: String): Option[URL] = {
    Option(clazz.getResource(name))
  }
}
