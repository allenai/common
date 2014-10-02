package org.allenai.common

import org.allenai.common.Config._

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

/** Represents the version of this component. Should be built with the `fromResources` method on the
  * companion object.
  *
  * @param  gitSha1  the output of `git sha1` in the repository.
  * @param  gitRemotes  the urls of all the remotes
  * @param  commitDate commit date in milliseconds
  * @param  artifactVersion  the version of the artifact in the build.
  */
case class Version(
    val gitSha1: String,
    val commitDate: Long,
    val gitRemotes: Seq[String],
    val artifactVersion: String) {
  @deprecated("Use gitSha1 instead.", "2014.09.09-1-SNAPSHOT")
  def git = gitSha1

  @deprecated("Use artifactVersion instead.", "2014.09.09-1-SNAPSHOT")
  def artifact = artifactVersion

  def githubProjectUrl(user: String): Option[String] = {
    val sshRegex = """git@github.com:(\w+)/(\w+).git""".r
    val httpsRegex = """https://github.com/(\w+)/(\w+).git""".r

    gitRemotes.collect {
      case sshRegex(u, repo) if u == user => s"http://github.com/$user/$repo"
      case httpsRegex(u, repo) if u == user => s"http://github.com/$user/$repo"
    }.headOption
  }

  def githubCommitUrl(user: String): Option[String] = {
    githubProjectUrl(user).map { base =>
      base + "/commit/" + gitSha1
    }
  }
}

object Version {
  /** Load Version from resources injected by the in-house Version sbt plugin.
    *
    * The parameters are used to build a package name in which the injected
    * configuration files are to be found.
    *
    * @param  org  the sbt organization, or group id, the calling code is in
    * @param  name  the sbt name, or artifact id, the calling code is in
    */
  def fromResources(org: String, name: String) = {
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
    val gitRemotes = gitConf.getStringList("remotes").asScala

    Version(gitVersion, gitCommitDate, gitRemotes, artifactVersion)
  }

  import spray.json.DefaultJsonProtocol._
  implicit val versionJsonFormat = jsonFormat4(Version.apply)
}
