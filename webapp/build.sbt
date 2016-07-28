import Dependencies._

name := "common-webapp"

libraryDependencies ++= Seq(
  akkaActor,
  okHttp,
  sprayClient,
  sprayJson,
  sprayRouting,
  typesafeConfig,
  sprayTestkit % Test
)

dependencyOverrides ++= Set(
  // Override needed because spray testkit declares dependency on an older version of akka
  akkaTestkit,
  pegdown,
  scalaCheck,
  scalaReflection(defaultScalaVersion)
)
