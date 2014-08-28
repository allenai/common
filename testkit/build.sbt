import Dependencies._

name := "common-testkit"

libraryDependencies ++= Seq(
  akkaModule("actor") % "provided",
  akkaModule("actor") % "test",
  akkaModule("testkit"),
  scalaCheck,
  scalaTest,
  pegdown
)
