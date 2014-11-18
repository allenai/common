import Dependencies._

name := "common-webapp"

libraryDependencies ++= Seq(
  akkaModule("actor"),
  slf4j,
  sprayJson,
  sprayRouting,
  sprayTestkit % "test",
  typesafeConfig)

dependencyOverrides ++= Set(akkaModule("testkit"))
