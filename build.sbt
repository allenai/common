BuildSettings.baseSettings

organization := "org.allenai.common"

name := "common"

version := "0.0.3-SNAPSHOT"

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "com.typesafe" % "config" % "1.2.0",
  "io.spray" %%  "spray-json" % "1.2.6"
)

conflictManager := ConflictManager.strict

lazy val testkit = project.in(file("testkit"))

lazy val common = project.in(file(".")).dependsOn(testkit % "test->compile").aggregate(testkit)
