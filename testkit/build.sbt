import Dependencies._

name := "testkit"

version := "0.0.3-SNAPSHOT"

libraryDependencies ++= Seq(
  akkaModule("actor") % "provided",
  akkaModule("actor") % "test",
  akkaModule("testkit"),
  scalaCheck,
  scalaTest
)
