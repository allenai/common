import Dependencies._

name := "common-core"

libraryDependencies ++= Seq(
  apacheLang3,
  datastore,
  Logging.logbackClassic,
  Logging.logbackCore,
  Logging.slf4jApi,
  openCsv,
  sprayJson,
  typesafeConfig
)
