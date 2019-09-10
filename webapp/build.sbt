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

dependencyOverrides += akkaTestkit
dependencyOverrides += pegdown
dependencyOverrides += scalaCheck
