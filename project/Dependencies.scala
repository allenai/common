import sbt._

/** Object holding the dependencies Common has, plus resolvers and overrides. */
object Dependencies {
  //val Resolvers = Seq(
  //  "spray repo" at "http://repo.spray.io",
  //  Resolver.sonatypeRepo("snapshots"))

  val Resolvers = Seq(
    "AllenAI Snapshots" at "http://utility.allenai.org:8081/nexus/content/repositories/snapshots",
    "AllenAI Releases" at "http://utility.allenai.org:8081/nexus/content/repositories/releases",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "IESL Releases" at "http://dev-iesl.cs.umass.edu/nexus/content/groups/public")

  val Overrides = Set("org.scala-lang" % "scala-library" % "2.10.4")

  def akkaModule(id: String) = "com.typesafe.akka" %% s"akka-${id}" % "2.3.2"

  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.4"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.0"
  val scalaReflection = "org.scala-lang" % "scala-reflect" % "2.10.4"
  val pegdown = "org.pegdown" % "pegdown" % "1.4.2"
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.6"
  val sprayJson = "io.spray" %% "spray-json" % "1.2.6"
  val sprayRouting = "io.spray" % "spray-routing" % "1.3.1"
  val typesafeConfig = "com.typesafe" % "config" % "1.2.0"
  val awsJavaSdk = "com.amazonaws" % "aws-java-sdk" % "1.8.9.1"
  val commonsIO = "commons-io" % "commons-io" % "2.4"
  val scopt = "com.github.scopt" %% "scopt" % "3.2.0"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val logbackCore = "ch.qos.logback" % "logback-core" % "1.1.2"
}
