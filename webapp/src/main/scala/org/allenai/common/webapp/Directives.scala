package org.allenai.common.webapp

import spray.http.{ HttpHeaders, HttpOrigin, SomeOrigins }
import spray.routing.Directive0
import spray.routing.Directives._

/** Helper spray directives. */
trait Directives {
  /** Directive providing CORS header support. This should be included in any application serving
    * a REST API that's queried cross-origin (from a different host than the one serving the API).
    * See http://www.w3.org/TR/cors/ for full specification.
    * @param allowedHostnames the set of hosts that are allowed to query the API. These should
    * not include the scheme or port; they're matched only against the hostname of the Origin
    * header.
    */
  def allowHosts(allowedHostnames: Set[String]): Directive0 = mapInnerRoute { innerRoute =>
    // Conditionally responds with "allowed" CORS headers, if the request origin's host is in the
    // allowed set, or if the request doesn't have an origin.
    optionalHeaderValueByType[HttpHeaders.Origin]() { originOption =>
      // If Origin is set and the host is in our allowed set, add CORS headers and pass through.
      originOption flatMap {
        case HttpHeaders.Origin(list) => list.find {
          case HttpOrigin(_, HttpHeaders.Host(hostname, _)) => allowedHostnames.contains(hostname)
        }
      } map { goodOrigin =>
        respondWithHeaders(
          Headers.AccessControlAllowHeadersAll,
          HttpHeaders.`Access-Control-Allow-Origin`(SomeOrigins(Seq(goodOrigin)))
        ) {
            options {
              complete {
                ""
              }
            } ~
              innerRoute
          }
      } getOrElse {
        // Else, pass through without headers.
        innerRoute
      }
    }
  }

  def allowHosts(allowedHostnames: String*): Directive0 = allowHosts(allowedHostnames.toSet)
}
object Directives extends Directives
