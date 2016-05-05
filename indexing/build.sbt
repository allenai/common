import Dependencies._

name := "common-indexing"

libraryDependencies ++= Seq(
  elasticSearch,
  scopt,
  sprayClient,
  typesafeConfig,
  datastore,
  nlpstack("segment")
)
