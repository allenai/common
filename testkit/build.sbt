import Dependencies._

name := "common-testkit"

libraryDependencies ++= Seq(
  akkaModule("actor") % Provided,
  akkaModule("actor") % Test,
  akkaModule("testkit"),
  scalaCheck,
  scalaTest,
  pegdown
)
