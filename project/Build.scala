import sbt._
import Keys._

object CommonBuild extends Build {
  val inheritedSettings = Format.settings ++ Publish.settings ++ TravisPublisher.settings

  val buildSettings = inheritedSettings ++ Seq(
    organization := "org.allenai.common",
    crossScalaVersions := Seq("2.10.4"),
    scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
    scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature"),
    conflictManager := ConflictManager.strict,
    dependencyOverrides ++= Dependencies.Overrides,
    resolvers ++= Dependencies.Resolvers
  )

  lazy val testkit = Project(
    id = "testkit",
    base = file("testkit"),
    settings = buildSettings)

  lazy val common = Project(
    id = "common",
    base = file("."),
    settings = buildSettings
  ).dependsOn(testkit % "test->compile").aggregate(testkit)
}
