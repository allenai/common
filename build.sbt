import Dependencies._

lazy val scala211 = "2.11.12"
lazy val scala212 = "2.12.9"
lazy val scala213 = "2.13.0"
lazy val supportedScalaVersions = List(scala211)

ThisBuild / organization := "org.allenai.common"
ThisBuild / version      := "1.4.11-SNAPSHOT"
ThisBuild / scalaVersion := scala211

lazy val common = (project in file("."))
    .aggregate(cache,
      core,
      guice,
      indexing,
      testkit,
      webapp)
    .settings(
      crossScalaVersions := Nil,
      publish / skip := true,
      buildSettings
    )

lazy val buildSettings = Seq(
  organization := "org.allenai.common",
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/allenai/common")),
  apiURL := Some(url("https://allenai.github.io/common/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/allenai/common"),
      "https://github.com/allenai/common.git"
    )
  ),
  pomExtra := (
      <developers>
        <developer>
          <id>allenai-dev-role</id>
          <name>Allen Institute for Artificial Intelligence</name>
          <email>dev-role@allenai.org</email>
        </developer>
      </developers>),
  bintrayPackage := s"${organization.value}:${name.value}_${scalaBinaryVersion.value}"
)

lazy val cache = Project(id = "cache", base = file("cache"))
    .settings(
      crossScalaVersions := supportedScalaVersions,
      buildSettings
    )
    .dependsOn(core, testkit % "test->compile")

lazy val core = Project(id = "core", base = file("core"))
    .settings(
      crossScalaVersions := supportedScalaVersions,
      buildSettings
    )
    .dependsOn(testkit % "test->compile")

lazy val guice = Project(id = "guice", base = file("guice"))
    .settings(
      crossScalaVersions := supportedScalaVersions,
      buildSettings
    )
    .dependsOn(core, testkit % "test->compile")

lazy val indexing = Project(id = "indexing", base = file("indexing"))
    .settings(
      crossScalaVersions := supportedScalaVersions,
      buildSettings
    )
    .dependsOn(core, testkit % "test->compile")

lazy val testkit = Project(id = "testkit", base = file("testkit"))
    .settings(
      crossScalaVersions := supportedScalaVersions,
      buildSettings
    )

lazy val webapp = Project(id = "webapp", base = file("webapp"))
    .settings(
      crossScalaVersions := supportedScalaVersions,
      buildSettings,
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      )
    )
    .dependsOn(core, testkit % "test->compile")


