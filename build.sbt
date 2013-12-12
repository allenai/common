Format.settings

Nexus.settings

Version.settings

organization := "org.allenai.common"

name := "common"

crossScalaVersions := Seq("2.10.3")

scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head }

scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature")

libraryDependencies ++= Seq(
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
    "org.scalacheck" %% "scalacheck" % "1.10.1" % "test"
  )
