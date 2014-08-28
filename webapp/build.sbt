import Dependencies._

name := "common-webapp"

libraryDependencies ++= Seq(
  akkaModule("actor"),
  sl4j,
  sprayJson,
  sprayRouting,
  typesafeConfig)
