import sbt._
import Keys._

import scala.util.Try

/** Publish settings, for a public repository. */
object Publish {
  lazy val settings = Seq(
    publishTo <<= isSnapshot { isSnap =>
      if (isSnap) {
        Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      } else {
        Some("releases" at "https://oss.sonatype.org/content/repositories/releases")
      }
    },
    licenses := Seq(
      "Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
    )
  )
}
