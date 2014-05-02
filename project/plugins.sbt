addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.4")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.1")

resolvers += "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

addSbtPlugin("org.allenai.plugins" % "sbt-travis-publisher" % "2014.2.24-1-SNAPSHOT")
