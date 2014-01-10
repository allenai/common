addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.4")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.1")

resolvers += "allenai nexus repository" at "http://utility.allenai.org:8081/nexus/content/repositories/releases"

credentials += Credentials("Sonatype Nexus Repository Manager", "utility.allenai.org", "deployment", "answermyquery")

addSbtPlugin("org.allenai.plugins" % "sbt-travis-publisher" % "0.2.0")
