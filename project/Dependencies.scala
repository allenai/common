import sbt._

/** Object holding the dependencies Common has, plus resolvers and overrides. */
object Dependencies {

  val apacheLang3 = "org.apache.commons" % "commons-lang3" % "3.4"

  val awsJavaSdk = ("com.amazonaws" % "aws-java-sdk" % "1.8.9.1")
    .exclude("commons-logging", "commons-logging")

  val commonsIO = "commons-io" % "commons-io" % "2.4"

  val elasticSearch = "org.elasticsearch" % "elasticsearch" % "2.3.3"

  val jedis = "redis.clients" % "jedis" % "2.7.2"

  val mockJedis = "com.fiftyonred" % "mock-jedis" % "0.4.0"

  def nlpstack(component: String) = ("org.allenai.nlpstack" %% s"nlpstack-${component}" % "1.6")
    .exclude("commons-logging", "commons-logging")

  val okHttp = "com.squareup.okhttp3" % "okhttp" % "3.4.1"

  val openCsv = "net.sf.opencsv" % "opencsv" % "2.1"

  val pegdown = "org.pegdown" % "pegdown" % "1.4.2"

  val scalaGuice = "net.codingwell" %% "scala-guice" % "4.2.6"

  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.0"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  val sprayJson = "io.spray" %% "spray-json" % "1.3.5"

  val typesafeConfig = "com.typesafe" % "config" % "1.2.1"

  val scopt = "com.github.scopt" %% "scopt" % "3.7.1"

  object Logging {
    val slf4jVersion = "1.7.28"
    val logbackVersion = "1.2.3"
    // The logging API to use. This should be the only logging dependency of any API artifact
    // (anything that's going to be depended on outside of this SBT project).
    val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion
    val logbackCore = "ch.qos.logback" % "logback-core" % logbackVersion
    val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion

    val loggingDependencyOverrides = Seq(
      Logging.slf4jApi,
      Logging.logbackCore,
      Logging.logbackClassic
    )
  }

}
