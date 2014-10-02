package org.allenai.common

import org.allenai.common.Config._

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

/** Represents a git version.
  * @param  sha1  the output of `git sha1` in the repository
  * @param  commitDate commit date in milliseconds
  * @param  repoUrl  the url of the git repo
  */
case class GitVersion(sha1: String, commitDate: Long, repoUrl: Option[String]) {
  /** A URL pointing to the specific commit on GitHub. */
  def commitUrl: Option[String] = {
    repoUrl.map { base =>
      base + "/commit/" + sha1
    }
  }
}

object GitVersion {
  import spray.json.DefaultJsonProtocol._
  implicit val gitVersionFormat = jsonFormat3(GitVersion.apply)

  /** The GitHub project URL.
    * The remotes are searched for one with user "allenai" and then it's
    * transformed into a valid GitHub project URL.
    *
    * @returns  a URL to a GitHub repo, or None if no allenai remotes exist
    */
  def projectUrl(remotes: Seq[String], user: String): Option[String] = {
    val sshRegex = """git@github.com:([\w-]+)/([\w-]+).git""".r
    val httpsRegex = """https://github.com/([\w-]+)/([\w-]+).git""".r

    remotes.collect {
      case sshRegex(u, repo) if u == user => s"http://github.com/$user/$repo"
      case httpsRegex(u, repo) if u == user => s"http://github.com/$user/$repo"
    }.headOption
  }

  def create(sha1: String, commitDate: Long, remotes: Seq[String]) = {
    GitVersion(sha1, commitDate, projectUrl(remotes, "allenai"))
  }
}

/** Represents the version of this component. Should be built with the `fromResources` method on the
  * companion object.
  *
  * @param  git  the git version (commit information) of the build.
  * @param  artifactVersion  the version of the artifact in the build.
  */
case class Version(
    git: GitVersion,
    artifactVersion: String) {
  @deprecated("Use artifactVersion instead.", "2014.09.09-1-SNAPSHOT")
  def artifact = artifactVersion
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
    val sha1 = gitConf[String]("sha1")
    val commitDate = gitConf[Long]("date")
    val remotes = gitConf.getStringList("remotes").asScala

    Version(GitVersion.create(sha1, commitDate, remotes), artifactVersion)
  }

  import spray.json.DefaultJsonProtocol._
  implicit val versionJsonFormat = jsonFormat2(Version.apply)
}
