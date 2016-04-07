import Dependencies._

name := "common-caching"

libraryDependencies ++= Seq(
  jedis,
  mockJedis % Test,
  scalaGuice,
  sprayJson,
  typesafeConfig
)
