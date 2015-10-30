import Dependencies._

name := "common-indexing"

def nlpstack(component: String) =
  ("org.allenai.nlpstack" % s"nlpstack-${component}_2.11" % "1.6").exclude("commons-logging", "commons-logging")

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "1.7.1",
  scopt,
  sprayClient,
  typesafeConfig,
  "org.allenai" %% "datastore" % "2015.04.02-0",
  nlpstack("segment")
)
