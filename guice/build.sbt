import Dependencies._

name := "common-guice"

libraryDependencies ++= Seq(
  akkaActor,
  scalaGuice,
  typesafeConfig
)
