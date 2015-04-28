import sbtrelease.ReleaseStep
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.ReleaseKeys


bintray.Keys.repository in bintray.Keys.bintray in ThisBuild := "common"

bintray.Keys.bintrayOrganization in bintray.Keys.bintray in ThisBuild := Some("allenai")

val checkBranchIsNotMaster = { st: State =>
  val vcs = Project.extract(st).get(ReleaseKeys.versionControlSystem).getOrElse {
    sys.error("Aborting release. Working directory is not a repository of a recognized VCS.")
  }

  if (vcs.currentBranch == "master") {
    sys.error("Current branch is master.  At AI2, releases are done from another branch and " +
      "then merged into master via pull request.  Shippable, our continuous build system does " +
      "the actual publishing of the artifacts.")
  }

  st
}

ReleaseKeys.releaseProcess in ThisBuild := Seq[ReleaseStep](
  checkBranchIsNotMaster,
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
