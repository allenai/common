import sbt._
import Keys._

import scala.util.Try

/** Publish settings, for a public repository. */
object Publish {
  lazy val settings = Seq(
    credentials += Credentials("Sonatype Nexus Repository Manager",
                               "oss.sonatype.org",
                               "allenai-role",
                               System.getenv("NEXUS_PASS")),
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
      "Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
    )
  )
}
