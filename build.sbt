import Dependencies._

lazy val scala211 = "2.11.12"
lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.2"
lazy val supportedScalaVersions = List(scala211, scala212, scala213)

ThisBuild / organization := "org.allenai.common"
ThisBuild / version := "2.2.2"
ThisBuild / scalaVersion := scala213

lazy val spray = "spray" at "https://repo.spray.io/"
lazy val typesafeReleases = "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

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
  publishArtifact in (Compile, packageDoc) := false,
  pomIncludeRepository := { _ =>
    false
  },
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/allenai/common")),
  apiURL := Some(url("https://allenai.github.io/common/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/allenai/common"),
      "https://github.com/allenai/common.git"
    )
  ),
  pomExtra := (<developers>
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
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-Xlint",
    "-deprecation",
    "-feature",
    "-Xfatal-warnings"
  ),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  crossScalaVersions := supportedScalaVersions,
  unmanagedSourceDirectories.in(Compile) ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, x)) if x == 11 || x == 12 =>
        Seq(file(sourceDirectory.value.getPath + "/main/scala-2.11-2.12"))
      case Some((2, x)) if x == 13 => Seq(file(sourceDirectory.value.getPath + "/main/scala-2.13"))
      case _ => Seq.empty // dotty support would go here
    }
  }
)

// Not necessary for this repository but here as an example
inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)

lazy val common = (project in file("."))
  .aggregate(
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

lazy val core = Project(id = "core", base = file("core"))
  .settings(projectSettings, buildSettings)
  .dependsOn(testkit % "test->compile")

lazy val guice = Project(id = "guice", base = file("guice"))
  .settings(projectSettings, buildSettings)
  .dependsOn(core, testkit % "test->compile")

lazy val testkit = Project(id = "testkit", base = file("testkit"))
  .settings(projectSettings, buildSettings)
