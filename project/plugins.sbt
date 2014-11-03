lazy val ai2PluginsVersion = "2014.11.02-0"

addSbtPlugin("org.allenai.plugins" % "allenai-sbt-core-settings" % ai2PluginsVersion)

addSbtPlugin("org.allenai.plugins" % "allenai-sbt-release" % ai2PluginsVersion)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")
