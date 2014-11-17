package org.allenai.common.webapp

import shapeless.HNil
import spray.http.{ HttpHeaders, HttpOrigin, SomeOrigins }
import spray.routing.{ Directive0, Route }
import spray.routing.Directives._

/** Helper spray directives. */
object Directives {
  def jsonApi(allowedHostnames: String*): Directive0 = new JsonApi(allowedHostnames.toSet)
  def jsonApi(allowedHostnames: Set[String]): Directive0 = new JsonApi(allowedHostnames)

  class JsonApi(allowedHostnames: Set[String]) extends Directive0 {

    /** Conditionally responds with "allowed" CORS headers, if the request origin's host is in the
      * allowed set, or if the request doesn't have an origin.
      */
    override def happly(innerRoute: HNil => Route): Route = {
      optionalHeaderValueByType[HttpHeaders.Origin]() { originOption =>
        // If Origin is set and the host is in our allowed set, add CORS headers and pass through.
        originOption flatMap {
          case HttpHeaders.Origin(list) => list.find {
            case HttpOrigin(_, HttpHeaders.Host(hostname, _)) => allowedHostnames.contains(hostname)
          }
        } map { goodOrigin =>
          respondWithHeaders(Headers.AccessControlAllowHeadersAll,
            HttpHeaders.`Access-Control-Allow-Origin`(SomeOrigins(Seq(goodOrigin)))
          ) {
            options {
              complete {
                ""
              }
            } ~
            innerRoute(HNil)
          }
        } getOrElse {
          // Else, pass through without headers.
          innerRoute(HNil)
        }
      }
    }
  }
}
