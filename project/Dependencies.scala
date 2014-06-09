import sbt._

/** Object holding the dependencies Common has, plus resolvers and overrides. */
object Dependencies {
  val Resolvers = Seq(
    "spray repo" at "http://repo.spray.io",
    Resolver.sonatypeRepo("snapshots"))

  val Overrides = Set("org.scala-lang" % "scala-library" % "2.10.4")

  def akkaModule(id: String) = "com.typesafe.akka" %% s"akka-${id}" % "2.3.2"

  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.3"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.1.3"
  val sl4j = "org.slf4j" % "slf4j-api" % "1.7.5"
  val sprayJson = "io.spray" %% "spray-json" % "1.2.6"
  val sprayRouting = "io.spray" % "spray-routing" % "1.3.1"
  val typesafeConfig = "com.typesafe" % "config" % "1.2.0"
}
