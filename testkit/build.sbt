import Dependencies._

name := "common-testkit"

version := "2014.07.23-0-SNAPSHOT"

libraryDependencies ++= Seq(
  akkaModule("actor") % "provided",
  akkaModule("actor") % "test",
  akkaModule("testkit"),
  scalaCheck,
  scalaTest,
  pegdown
)
