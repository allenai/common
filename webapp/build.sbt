import Dependencies._

name := "common-webapp"

libraryDependencies ++= Seq(
  akkaActor,
  sprayJson,
  sprayRouting,
  sprayTestkit % "test",
  typesafeConfig
)

dependencyOverrides ++= Set(
  // Override needed because spray testkit declares dependency on an older version of akka
  akkaTestkit,
  scalaReflection(defaultScalaVersion),
  "org.pegdown" % "pegdown" % "1.4.2",
  "org.scalacheck" %% "scalacheck" % "1.11.4")

