BuildSettings.baseSettings

organization := "org.allenai.common"

name := "common"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.5"
)

lazy val testkit = project.in(file("testkit"))

lazy val common = project.in(file(".")).dependsOn(testkit % "test->compile")
