import sbt._

import org.allenai.plugins.CoreDependencies

/** Object holding the dependencies Common has, plus resolvers and overrides. */
object Dependencies extends CoreDependencies {

  val apacheLang3 = "org.apache.commons" % "commons-lang3" % "3.4"

  val awsJavaSdk = ("com.amazonaws" % "aws-java-sdk" % "1.8.9.1")
    .exclude("commons-logging", "commons-logging")

  val commonsIO = "commons-io" % "commons-io" % "2.4"

  val datastore = "org.allenai" %% "datastore" % "1.0.0"

  val elasticSearch = "org.elasticsearch" % "elasticsearch" % "1.7.1"

  val jedis = "redis.clients" % "jedis" % "2.7.2"

  val mockJedis = "com.fiftyonred" % "mock-jedis" % "0.4.0"

  def nlpstack(component: String) = ("org.allenai.nlpstack" % s"nlpstack-${component}_2.11" % "1.6")
    .exclude("commons-logging", "commons-logging")

  val openCsv = "net.sf.opencsv" % "opencsv" % "2.1"

  val pegdown = "org.pegdown" % "pegdown" % "1.4.2"

  val scalaGuice = "net.codingwell" %% "scala-guice" % "4.0.1"

  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.4"

  def scalaReflection(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion

  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.1"
}
