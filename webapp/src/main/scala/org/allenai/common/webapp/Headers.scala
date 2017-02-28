package org.allenai.common.webapp

import spray.http.{HttpHeaders, HttpMethods}

/** Helpers for setting HTTP headers. */
object Headers {
  /** Allows any reasonable header to be sent cross-site. */
  val AccessControlAllowHeadersAll = HttpHeaders.`Access-Control-Allow-Headers`(
    Seq("Origin", "X-Requested-With", "Content-Type", "Accept")
  )
  val AccessControlAllowMethodsCommon = HttpHeaders.`Access-Control-Allow-Methods`(
    Seq(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE)
  )
}
