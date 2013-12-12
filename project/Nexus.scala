import sbt._
import Keys._

object Nexus {
  lazy val settings = Seq(
    credentials += Credentials("Sonatype Nexus Repository Manager",
                               "54.200.244.75",
                               "deployment",
                               "answermyquery")
  )
}
