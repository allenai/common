package org.allenai.common

import java.net.MalformedURLException
import java.net.URL
import scala.util.control.Exception._

/** Util methods for java.net.URL. */
object UrlUtil {
  def parse(url: String): Option[URL] =
    catching(classOf[MalformedURLException]) opt new URL(url)
}

