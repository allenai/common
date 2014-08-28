import sbt._
import Keys._

import scala.util.Try

/** Publish settings, for a public repository. */
object Publish {
  val nexusHost = "utility.allenai.org"
  val nexus = s"http://${nexusHost}:8081/nexus/content/repositories/"

  lazy val settings = Seq(
    publishTo := {
      if(isSnapshot.value)
        Some("snapshots" at nexus + "snapshots")
      else
        Some("releases"  at nexus + "releases")
    },
    licenses := Seq(
      "Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
    )
  )
}
