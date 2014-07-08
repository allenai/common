resolvers += Resolver.url(
  "allenai-bintray-sbt-plugins",
  url("http://dl.bintray.com/content/allenai/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.allenai.plugins" % "allenai-sbt-format" % "2014.07.02")

addSbtPlugin("org.allenai.plugins" % "allenai-sbt-travis-publisher" % "2014.07.02")
