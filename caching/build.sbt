import Dependencies._

name := "common-caching"

libraryDependencies ++= Seq(
  jedis,
  mockJedis,
  scalaGuice,
  sprayJson,
  typesafeConfig
)
