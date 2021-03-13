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
      organization := "org.allenai.common",
      CodeArtifactKeys.codeArtifactUrl := "https://org-allenai-s2-896129387501.d.codeartifact.us-west-2.amazonaws.com/maven/private",
      dependencyOverrides ++= Logging.loggingDependencyOverrides,
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
      javaOptions += s"-Dlogback.appname=${name.value}",
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
      crossScalaVersions := SUPPORTED_SCALA_VERSIONS,
      unmanagedSourceDirectories.in(Compile) ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, x)) if x == 11 || x == 12 =>
            Seq(file(sourceDirectory.value.getPath + "/main/scala-2.11-2.12"))
          case Some((2, x)) if x == 13 =>
            Seq(file(sourceDirectory.value.getPath + "/main/scala-2.13"))
          case _ => Seq.empty // dotty support would go here
        }
      }
    )
}
