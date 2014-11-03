import Dependencies._

name := "common-datastore"

libraryDependencies ++= Seq(jclOverSlf4j, slf4j, awsJavaSdk, commonsIO)
