import Dependencies._

// Override the name "core" to keep the old artifact name.
name := "common"

version := "2014.06.09-1-SNAPSHOT"

libraryDependencies ++= Seq(sl4j, sprayJson, typesafeConfig)
