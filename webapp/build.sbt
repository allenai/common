import Dependencies._

name := "common-webapp"

version := "2014.06.10-0-SNAPSHOT"

libraryDependencies ++= Seq(
  akkaModule("actor"),
  sl4j,
  sprayJson,
  sprayRouting,
  typesafeConfig)
