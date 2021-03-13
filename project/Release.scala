import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.autoImport._

object Release {

  def settings: Seq[ReleaseStep] = Seq(
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    releaseStepCommandAndRemaining("+test"),
    setReleaseVersion,
    // commitReleaseVersion,
    // tagRelease,
    releaseStepCommandAndRemaining("+codeArtifactPublish")
    // setNextVersion,
    // commitNextVersion,
    // pushChanges
  )
}
