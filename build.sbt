lazy val common = project
  .in(file("."))
  .aggregate(
    core,
    guice,
    testkit
  )
  .settings(Release.noPublish)

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
