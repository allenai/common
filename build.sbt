lazy val common = project
  .in(file("."))
  .aggregate(
    core,
    guice,
    testkit
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    // crossScalaVersions must be set to Nil on the aggregating project
    // in order to avoid double publishing.
    // See: https://www.scala-sbt.org/1.x/docs/Cross-Build.html#Cross+building+a+project+statefully
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val core = project
  .in(file("core"))
  .dependsOn(testkit % "test->compile")
  .settings(Release.settings)

lazy val guice = project
  .in(file("guice"))
  .dependsOn(core, testkit % "test->compile")
  .settings(Release.settings)

lazy val testkit = project
  .in(file("testkit"))
  .settings(Release.settings)
