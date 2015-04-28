import Dependencies._

name := "common-core"

libraryDependencies ++= Seq(sprayJson, typesafeConfig) ++
  Seq(Logging.slf4jApi, Logging.logbackCore, Logging.logbackClassic)
