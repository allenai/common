import sbt._
import Keys._

import scala.util.Try

object Nexus {
  val nexusHost = "54.200.244.75"
  lazy val settings = Seq(
    credentials += Credentials("Sonatype Nexus Repository Manager",
                               "54.200.244.75",
                               "deployment",
                               "answermyquery"),
    publishTo <<= version { (v: String) =>
      val nexus = s"http://${nexusHost}:8081/nexus/content/repositories/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "snapshots")
      else if(Try(sys.env("TRAVIS")).getOrElse("false") == "true")
        None
      else
        Some("releases"  at nexus + "releases")
      }
  )
}
