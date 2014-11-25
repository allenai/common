import Dependencies._

name := "common-webapp"

libraryDependencies ++= Seq(
  akkaActor,
  sprayJson,
  sprayRouting,
  sprayTestkit % "test",
  typesafeConfig
)

// Override needed because spray testkit declares dependency on an older version of akka
dependencyOverrides += akkaTestkit
