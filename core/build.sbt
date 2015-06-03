import Dependencies._

name := "common-core"

libraryDependencies ++= Seq(
  sprayJson,
  typesafeConfig,
  Logging.logbackClassic,
  Logging.logbackCore,
  Logging.slf4jApi
)
