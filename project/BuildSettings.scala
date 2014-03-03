import sbt._
import Keys._

object BuildSettings {
  def baseSettings = Format.settings ++ Nexus.settings ++ TravisPublisher.settings ++ Seq(
    crossScalaVersions := Seq("2.10.3"),
    scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
    scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature"),
    organization := "org.allenai.common"
  )
}
