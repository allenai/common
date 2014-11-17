import org.allenai.plugins.ReleasePlugin
import org.allenai.plugins.CoreRepositories
import sbtrelease.ReleasePlugin._
import com.typesafe.sbt.pgp.PgpKeys._

import sbt._
import Keys._

object CommonBuild extends Build {
  val buildSettings = releaseSettings ++ Seq(
    organization := "org.allenai.common",
    crossScalaVersions := Seq("2.10.4"),
    scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
    scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature"),
    conflictManager := ConflictManager.strict,
    dependencyOverrides ++= Dependencies.Overrides,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    licenses := Seq("Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/allenai/common")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/allenai/common"),
      "https://github.com/allenai/common.git")),
    ReleaseKeys.publishArtifactsAction := publishSigned.value,
    pomExtra := (
      <developers>
        <developer>
          <id>allenai-dev-role</id>
          <name>Paul Allen Institute for Artificial Intelligence</name>
          <email>dev-role@allenai.org</email>
        </developer>
      </developers>)
  ) ++ CoreRepositories.PublishTo.sonatype

  lazy val testkit = Project(
    id = "testkit",
    base = file("testkit"),
    settings = buildSettings).enablePlugins(ReleasePlugin)

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = buildSettings
  ).enablePlugins(ReleasePlugin).dependsOn(testkit % "test->compile")

  lazy val webapp = Project(
    id = "webapp",
    base = file("webapp"),
    settings = buildSettings
  ).enablePlugins(ReleasePlugin).dependsOn(core)

  lazy val pipeline = Project(
    id = "pipeline",
    base = file("pipeline"),
    settings = buildSettings
  ).dependsOn(testkit % "test->compile", core).enablePlugins(ReleasePlugin)

  lazy val common = Project(id = "common", base = file(".")).settings(
    // Don't publish a jar for the root project.
    publishArtifact := false, publishTo := Some("dummy" at "nowhere"), publish := { }, publishLocal := { }
  ).aggregate(webapp, core, testkit, pipeline).enablePlugins(ReleasePlugin)
}
