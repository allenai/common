import sbt._

/** Object holding the dependencies Common has, plus resolvers and overrides. */
object Dependencies {
  def akkaModule(id: String) = "com.typesafe.akka" %% s"akka-${id}" % "2.3.2"

  def scalaReflection(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion

  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.4"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.0"
  val pegdown = "org.pegdown" % "pegdown" % "1.4.2"
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.6"
  val sprayJson = "io.spray" %% "spray-json" % "1.2.6"
  val sprayRouting = "io.spray" %% "spray-routing" % "1.3.1"
  val sprayTestkit = "io.spray" %% "spray-testkit" % "1.3.1"
  val typesafeConfig = "com.typesafe" % "config" % "1.2.0"
  val awsJavaSdk = "com.amazonaws" % "aws-java-sdk" % "1.8.9.1" exclude("commons-logging", "commons-logging")
  val commonsIO = "commons-io" % "commons-io" % "2.4"
  val scopt = "com.github.scopt" %% "scopt" % "3.2.0"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val logbackCore = "ch.qos.logback" % "logback-core" % "1.1.2"

  // Bridge jcl to slf4j (needed to bridge logging from awsJavaSdk to slf4j)
  val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % "1.7.7"
}
