import Dependencies._

version := "2014.06.09-1-SNAPSHOT"

libraryDependencies ++= Seq(
  akkaModule("actor"),
  sl4j,
  sprayJson,
  sprayRouting,
  typesafeConfig)
