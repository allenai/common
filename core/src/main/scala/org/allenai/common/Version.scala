package org.allenai.common

import org.allenai.common.Config._

import com.typesafe.config.ConfigFactory

/** Represents the version of this component. Should be built with the `fromResources` method on the
  * companion object.
  *
  * @param  git  the output of `git sha1` in the repository.
  * @param  artifact  the version of the artifact in the build.
  * @param  commitDate commit date in milliseconds
  */
case class Version(git: String, artifact: String, commitDate: Long)

object Version {
  /** Load Version from resources injected by the in-house Version sbt plugin.
    *
    * The parameters are used to build a package name in which the injected
    * configuration files are to be found.
    *
    * @param  org  the sbt organization, or group id, the calling code is in
    * @param  name  the sbt name, or artifact id, the calling code is in
    */
  def fromResources(org: String, name: String): Version = {
    val pkg = "/" + org + "/" + name.replaceAll("-", "")
    val artifactConfUrl = this.getClass.getResource(pkg + "/artifact.conf")
    val gitConfUrl = this.getClass.getResource(pkg + "/git.conf")

    require(artifactConfUrl != null, "Could not find artifact.conf in " + pkg + ".")
    require(gitConfUrl != null, "Could not find git.conf in " + pkg + ".")

    val artifactConf = ConfigFactory.parseURL(artifactConfUrl)
    val gitConf = ConfigFactory.parseURL(gitConfUrl)

    val artifactVersion = artifactConf[String]("version")
    val gitVersion = gitConf[String]("sha1")
    val gitCommitDate = gitConf[Long]("date")

    Version(gitVersion, artifactVersion, gitCommitDate)
  }

  import spray.json.DefaultJsonProtocol._
  implicit val versionJsonFormat = jsonFormat3(Version.apply)
}
