import Dependencies._

lazy val scala211 = "2.11.12"
lazy val scala212 = "2.12.9"
lazy val scala213 = "2.13.0" // Not supported yet (collections changes required)
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / organization := "org.allenai.common"
ThisBuild / version      := "2.0.0-SNAPSHOT"
ThisBuild / scalaVersion := scala212

lazy val spray = "spray" at "http://repo.spray.io/"
lazy val typesafeReleases = "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

lazy val projectSettings = Seq(
  resolvers ++= Seq(
    Resolver.bintrayRepo("allenai", "maven"),
    spray,
    Resolver.jcenterRepo,
    typesafeReleases
  ),
  dependencyOverrides ++= Logging.loggingDependencyOverrides,
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
  bintrayPackage := s"${organization.value}:${name.value}_${scalaBinaryVersion.value}",
  bintrayOrganization := Some("allenai"),
  bintrayRepository := "maven"
)

lazy val buildSettings = Seq(
  javaOptions += s"-Dlogback.appname=${name.value}",
  scalacOptions ++= Seq("-target:jvm-1.8", "-Xlint", "-deprecation", "-feature"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  crossScalaVersions := supportedScalaVersions,
)

// Not necessary for this repository but here as an example
inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)

lazy val common = (project in file("."))
    .aggregate(
      cache,
      core,
      guice,
      testkit
    )
    .configs(IntegrationTest)
    .settings(
      Defaults.itSettings,
      crossScalaVersions := Nil,
      publish / skip := true,
      buildSettings
    )

lazy val cache = Project(id = "cache", base = file("cache"))
    .settings(projectSettings, buildSettings)
    .dependsOn(core, testkit % "test->compile")

lazy val core = Project(id = "core", base = file("core"))
    .settings(projectSettings, buildSettings)
    .dependsOn(testkit % "test->compile")

lazy val guice = Project(id = "guice", base = file("guice"))
    .settings(projectSettings, buildSettings)
    .dependsOn(core, testkit % "test->compile")

lazy val testkit = Project(id = "testkit", base = file("testkit"))
    .settings(projectSettings, buildSettings)
