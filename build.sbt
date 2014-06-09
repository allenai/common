import Dependencies._

name := "common"

version := "2014.04.28-SNAPSHOT"

libraryDependencies ++= Seq(
  akkaModule("actor"),
  sl4j,
  sprayJson,
  sprayRouting,
  typesafeConfig)
