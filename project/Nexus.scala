import sbt._
import Keys._

import scala.util.Try
import java.net.URL

object Nexus {
  val nexusHost = "54.200.244.75"
  lazy val settings = Seq(
    credentials += Credentials("Sonatype Nexus Repository Manager",
                               "oss.sonatype.org",
                               "marksai2",
                               "answermyquery"),
    publishTo <<= isSnapshot { isSnap =>
      if (isSnap) {
        Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      } else if (Try(sys.env("TRAVIS")).getOrElse("false") == "true") {
        None
      } else {
        Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
      }
    },
    licenses := Seq(
      "BSD 3-Clause" -> new URL("http://opensource.org/licenses/BSD-3-Clause")
    )
  )
}
