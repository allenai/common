package org.allenai.common.webapp

import spray.http.HttpHeaders

/** Helpers for setting HTTP headers. */
object Headers {
  /** Allows any reasonable header to be sent cross-site. */
  val AccessControlAllowHeadersAll = HttpHeaders.`Access-Control-Allow-Headers`(
    Seq("Origin", "X-Requested-With", "Content-Type", "Accept")
  )
}
