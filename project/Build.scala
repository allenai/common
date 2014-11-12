import org.allenai.sbt.release.AllenaiReleasePlugin
import org.allenai.sbt.core.CoreSettings
import sbtrelease.ReleasePlugin._

import sbt._
import Keys._

object CommonBuild extends Build {
  val buildSettings = Seq(
    organization := "org.allenai.common",
    crossScalaVersions := Seq("2.10.4"),
    scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
    scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature"),
    conflictManager := ConflictManager.strict,
    dependencyOverrides ++= Dependencies.Overrides,
    licenses := Seq(
      "Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
    )
  ) ++ 
    CoreSettings.publishToRepos.ai2.publicRepo ++
    releaseSettings

  lazy val testkit = Project(
    id = "testkit",
    base = file("testkit"),
    settings = buildSettings).enablePlugins(AllenaiReleasePlugin)

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = buildSettings
  ).enablePlugins(AllenaiReleasePlugin).dependsOn(testkit % "test->compile")

  lazy val webapp = Project(
    id = "webapp",
    base = file("webapp"),
    settings = buildSettings
  ).enablePlugins(AllenaiReleasePlugin).dependsOn(core)

  lazy val pipeline = Project(
    id = "pipeline",
    base = file("pipeline"),
    settings = buildSettings
  ).dependsOn(testkit % "test->compile", core).enablePlugins(AllenaiReleasePlugin)

  lazy val common = Project(id = "common", base = file(".")).settings(
    // Don't publish a jar for the root project.
    publishTo := Some("dummy" at "nowhere"), publish := { }, publishLocal := { }
  ).aggregate(webapp, core, testkit, pipeline).enablePlugins(AllenaiReleasePlugin)
}
