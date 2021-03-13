import ScalaVersions._

import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

import sbt._
import sbt.Keys._

object Release {

  def settings = Seq(
    organization := "org.allenai.common",
    crossScalaVersions := SUPPORTED_SCALA_VERSIONS,
    releaseProcess := releaseSteps,
    Compile / unmanagedSourceDirectories ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, x)) if x == 11 || x == 12 =>
          Seq(file(sourceDirectory.value.getPath + "/main/scala-2.11-2.12"))
        case Some((2, x)) if x == 13 =>
          Seq(file(sourceDirectory.value.getPath + "/main/scala-2.13"))
        case _ => Seq.empty // dotty support would go here
      }
    },
    Test / publishArtifact := false,
    Compile / packageDoc / publishArtifact := false,
    pomIncludeRepository := { _ => false },
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("https://github.com/allenai/common")),
    apiURL := Some(url("https://allenai.github.io/common/")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/allenai/common"),
        "https://github.com/allenai/common.git"
      )
    )
  )

  def releaseSteps: Seq[ReleaseStep] = Seq(
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    releaseStepCommandAndRemaining("+test"),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+codeArtifactPublish"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
}
