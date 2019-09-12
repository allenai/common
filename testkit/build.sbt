import Dependencies._

name := "common-testkit"

libraryDependencies ++= Seq(
  scalaCheck,
  scalaTest,
  pegdown
)
