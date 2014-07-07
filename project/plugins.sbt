resolvers += Resolver.url(
  "allenai-bintray-sbt-plugins",
  url("http://dl.bintray.com/content/allenai/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.allenai.plugins" % "sbt-format" % "2014.07.02")
