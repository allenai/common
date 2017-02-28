package org.allenai.common.webapp

import spray.http.{ HttpHeaders, HttpMethods }

/** Helpers for setting HTTP headers. */
object Headers {
  /** Allows any reasonable header to be sent cross-site. */
  val AccessControlAllowHeadersAll = HttpHeaders.`Access-Control-Allow-Headers`(
    Seq("Origin", "X-Requested-With", "Content-Type", "Accept")
  )
  val AccessControlAllowMethodsAll = HttpHeaders.`Access-Control-Allow-Methods`(
    Seq(
      HttpMethods.CONNECT,
      HttpMethods.DELETE,
      HttpMethods.GET,
      HttpMethods.HEAD,
      HttpMethods.OPTIONS,
      HttpMethods.PATCH,
      HttpMethods.POST,
      HttpMethods.PUT,
      HttpMethods.TRACE
    )
  )
}
