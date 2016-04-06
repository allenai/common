import Dependencies._

name := "common-core"

libraryDependencies ++= Seq(
  apacheLang3,
  jedis,
  Logging.logbackClassic,
  Logging.logbackCore,
  Logging.slf4jApi,
  mockJedis,
  sprayJson,
  typesafeConfig
)
