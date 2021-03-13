import Dependencies._
import ScalaVersions._

import sbt._
import sbt.Keys._
import codeartifact.CodeArtifactKeys

// Applies common settings to all subprojects
object GlobalPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def projectSettings =
    Seq(
      scalaVersion := SCALA_213,
      CodeArtifactKeys.codeArtifactUrl := "https://org-allenai-s2-896129387501.d.codeartifact.us-west-2.amazonaws.com/maven/private",
      dependencyOverrides ++= Logging.loggingDependencyOverrides,
      javaOptions += s"-Dlogback.appname=${name.value}",
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
    ) ++ Release.settings
}
