BuildSettings.baseSettings

name := "testkit"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0",
  "org.scalacheck" %% "scalacheck" % "1.10.1",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3" % "provided",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3"
)
