lazy val ai2PluginsVersion = "2014.10.02-0"

addSbtPlugin("org.allenai.plugins" % "allenai-sbt-style" % ai2PluginsVersion)

addSbtPlugin("org.allenai.plugins" % "allenai-sbt-version-injector" % ai2PluginsVersion)

addSbtPlugin("org.allenai.plugins" % "allenai-sbt-release" % ai2PluginsVersion)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")
