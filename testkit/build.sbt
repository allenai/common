import Dependencies._

name := "common-testkit"

version := "2014.06.10-0-SNAPSHOT"

libraryDependencies ++= Seq(
  akkaModule("actor") % "provided",
  akkaModule("actor") % "test",
  akkaModule("testkit"),
  scalaCheck,
  scalaTest
)
