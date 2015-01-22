import sbt._

import org.allenai.plugins.CoreDependencies

/** Object holding the dependencies Common has, plus resolvers and overrides. */
object Dependencies extends CoreDependencies {

  def scalaReflection(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion
  val pegdown = "org.pegdown" % "pegdown" % "1.4.2"
  val awsJavaSdk = "com.amazonaws" % "aws-java-sdk" % "1.8.9.1" exclude("commons-logging", "commons-logging")
  val commonsIO = "commons-io" % "commons-io" % "2.4"

  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.4"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.1"

}
