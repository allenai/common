package org.allenai.common.webapp

import org.allenai.common.testkit.UnitSpec

import spray.http.{ HttpHeader, HttpHeaders, HttpOrigin, SomeOrigins }
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

/** Tests for our custom directives. */
class DirectivesSpec extends UnitSpec with ScalatestRouteTest with HttpService {
  def actorRefFactory = system

  // Test route. Has API and non-API routes.
  // format: OFF
  val testRoute =
    get { path("foo") { complete { "foo" } } } ~
    Directives.allowHosts("localhost", "ari.dev.allenai.org", "ari.prod.allenai.org") {
      get { path("api") { complete { "api" } } }
    } ~
    Directives.allowHosts("localhost2") {
      get{ path("api2") { complete { "api2" } } }
    } ~
    get { path("bar") { complete { "bar" } } }
  // format: ON

  def allowOriginHeader(hostname: String): HttpHeader = {
    HttpHeaders.`Access-Control-Allow-Origin`(
      SomeOrigins(Seq(HttpOrigin("http", HttpHeaders.Host(hostname)))))
  }

  def addOriginHeader(origin: String): RequestTransformer = {
    addHeader(HttpHeaders.Origin(Seq(HttpOrigin("http", HttpHeaders.Host(origin)))))
  }

  "jsonApi" should "complete without CORS headers by default" in {
    Get("/api") ~> testRoute ~> check {
      header[HttpHeaders.`Access-Control-Allow-Origin`] should be(None)
      header[HttpHeaders.`Access-Control-Allow-Headers`] should be(None)
      responseAs[String] should be("api")
    }
  }
  it should "complete directives before the api directive" in {
    Get("/foo") ~> addOriginHeader("localhost") ~> testRoute ~> check {
      header[HttpHeaders.`Access-Control-Allow-Origin`] should be(None)
      header[HttpHeaders.`Access-Control-Allow-Headers`] should be(None)
      responseAs[String] should be ("foo")
    }
  }
  it should "complete directives after the api directive" in {
    Get("/bar") ~> addOriginHeader("localhost") ~> testRoute ~> check {
      header[HttpHeaders.`Access-Control-Allow-Origin`] should be(None)
      header[HttpHeaders.`Access-Control-Allow-Headers`] should be(None)
      responseAs[String] should be("bar")
    }
  }
  it should "complete with CORS headers when given a matching origin" in {
    Get("/api") ~> addOriginHeader("localhost") ~> testRoute ~> check {
      header[HttpHeaders.`Access-Control-Allow-Origin`] should be(
        Some(allowOriginHeader("localhost")))
      header[HttpHeaders.`Access-Control-Allow-Headers`] should be(
        Some(Headers.AccessControlAllowHeadersAll))
      responseAs[String] should be("api")
    }
  }
  it should "ignore ports and non-HTTP schemes" in {
    val origin = HttpOrigin("https", HttpHeaders.Host("ari.dev.allenai.org", 8081))
    Get("/api") ~> addHeader(HttpHeaders.Origin(Seq(origin))) ~> testRoute ~> check {
      header[HttpHeaders.`Access-Control-Allow-Origin`] should be(
        Some(HttpHeaders.`Access-Control-Allow-Origin`(SomeOrigins(Seq(origin)))))
      header[HttpHeaders.`Access-Control-Allow-Headers`] should be(
        Some(Headers.AccessControlAllowHeadersAll))
      responseAs[String] should be("api")
    }
  }
  it should "complete an OPTIONS request" in {
    Options("/api") ~> addOriginHeader("localhost") ~> testRoute ~> check {
      header[HttpHeaders.`Access-Control-Allow-Origin`] should be(
        Some(allowOriginHeader("localhost")))
      header[HttpHeaders.`Access-Control-Allow-Headers`] should be(
        Some(Headers.AccessControlAllowHeadersAll))
    }
  }
  it should "complete properly to a secondary api" in {
    Get("/api2") ~> addOriginHeader("localhost2") ~> testRoute ~> check {
      header[HttpHeaders.`Access-Control-Allow-Origin`] should be(
        Some(allowOriginHeader("localhost2")))
      header[HttpHeaders.`Access-Control-Allow-Headers`] should be(
        Some(Headers.AccessControlAllowHeadersAll))
      responseAs[String] should be("api2")
    }
  }
  it should "complete an OPTIONS request to a seconary api" in {
    Options("/api2") ~> addOriginHeader("localhost2") ~> testRoute ~> check {
      header[HttpHeaders.`Access-Control-Allow-Origin`] should be(
        Some(allowOriginHeader("localhost2")))
      header[HttpHeaders.`Access-Control-Allow-Headers`] should be(
        Some(Headers.AccessControlAllowHeadersAll))
    }
  }
}
