import sbtrelease._
import ReleaseStateTransformations._

lazy val buildSettings = Seq(
  organization := "org.allenai.common",
  crossScalaVersions := Seq("2.11.5"),
  scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  licenses := Seq("Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/allenai/common")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/allenai/common"),
    "https://github.com/allenai/common.git")),
  ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value,
  pomExtra := (
    <developers>
      <developer>
        <id>allenai-dev-role</id>
        <name>Allen Institute for Artificial Intelligence</name>
        <email>dev-role@allenai.org</email>
      </developer>
    </developers>),
  ReleaseKeys.releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion
  )
) ++ PublishTo.sonatype

lazy val testkit = Project(
  id = "testkit",
  base = file("testkit"),
  settings = buildSettings
).enablePlugins(LibraryPlugin)

lazy val core = Project(
  id = "core",
  base = file("core"),
  settings = buildSettings
).enablePlugins(LibraryPlugin).dependsOn(testkit % "test->compile")

lazy val webapp = Project(
  id = "webapp",
  base = file("webapp"),
  settings = buildSettings
).enablePlugins(LibraryPlugin).dependsOn(core, testkit % "test->compile")

lazy val common = Project(id = "common", base = file(".")).settings(
  // Don't publish a jar for the root project.
  publishArtifact := false, publishTo := Some("dummy" at "nowhere"), publish := { }, publishLocal := { }
).aggregate(webapp, core, testkit).enablePlugins(LibraryPlugin).settings(
  ReleaseKeys.releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion
  )
)
