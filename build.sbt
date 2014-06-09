BuildSettings.baseSettings

organization := "org.allenai.common"

name := "common"

version := "2014.04.28-SNAPSHOT"

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.2",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "com.typesafe" % "config" % "1.2.0",
  "io.spray" %% "spray-json" % "1.2.6",
  "io.spray" % "spray-routing" % "1.3.1"
)

dependencyOverrides ++= Set("org.scala-lang" % "scala-library" % "2.10.4")

conflictManager := ConflictManager.strict

lazy val testkit = project.in(file("testkit"))

lazy val common = project.in(file(".")).dependsOn(testkit % "test->compile").aggregate(testkit)
