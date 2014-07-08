import sbt._
import Keys._

import org.allenai.sbt.format._
import org.allenai.sbt.travispublisher._

object CommonBuild extends Build {
  val buildSettings = Seq(
    organization := "org.allenai.common",
    crossScalaVersions := Seq("2.10.4"),
    scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
    scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature"),
    conflictManager := ConflictManager.strict,
    dependencyOverrides ++= Dependencies.Overrides,
    resolvers ++= Dependencies.Resolvers
  ) ++ Publish.settings

  lazy val testkit = Project(
    id = "testkit",
    base = file("testkit"),
    settings = buildSettings).enablePlugins(TravisPublisherPlugin)

  lazy val common = Project(
    id = "core",
    base = file("core"),
    settings = buildSettings
  ).dependsOn(testkit % "test->compile").enablePlugins(TravisPublisherPlugin)

  lazy val webapp = Project(
    id = "webapp",
    base = file("webapp"),
    settings = buildSettings
  ).dependsOn(common).enablePlugins(TravisPublisherPlugin)

  lazy val root = Project(id = "root", base = file(".")).settings(
    // Don't publish a jar for the root project.
    publishTo := None, publish := { }, publishLocal := { }
  ).aggregate(webapp, common, testkit)
}
