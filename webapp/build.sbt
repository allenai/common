import Dependencies._

name := "common-webapp"

libraryDependencies ++= Seq(
  akkaActor,
  sprayJson,
  sprayRouting,
  sprayTestkit % Test,
  typesafeConfig
)

dependencyOverrides ++= Set(
  // Override needed because spray testkit declares dependency on an older version of akka
  akkaTestkit,
  scalaReflection(defaultScalaVersion),
  pegdown,
  scalaCheck
)

