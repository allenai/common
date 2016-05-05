import Dependencies._

name := "common-cache"

libraryDependencies ++= Seq(
  jedis,
  mockJedis % Test,
  sprayJson,
  typesafeConfig
)
