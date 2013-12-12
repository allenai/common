import sbt._
import Keys._

object Version {
  def versionWithGit: Seq[Setting[_]] =
    Seq(
      version in ThisBuild := Git.describe()
    )
}
