import sbt._
import Keys._

object Version {
  val inject = TaskKey[Seq[File]]("inject")

  def settings =
    // use 'git describe' for the version
    versionWithGit ++
    // add a file Artifact.scala containing the name and version
    generateArtifactSource ++
    // inject whenever code is compiled
    Seq(sourceGenerators in Compile <+= inject map identity)

  def versionWithGit: Seq[Setting[_]] =
    Seq(
      version in ThisBuild := Git.describe().trim
    )

  def generateArtifactSource: Seq[Setting[_]] = inject <<= (Keys.sourceManaged, Keys.organization, Keys.name, Keys.version) map {
    (sourceManaged: File, org: String, name: String, version: String) => {
      val file = sourceManaged / org / "Artifact.scala"

      val code = "package "+org+"\n\nobject Artifact {\n  val name = \""+name+"\"\n  val version = \""+version+"\"\n}"
      IO.write(file, code)
      Seq(file)
    }
  }
}
