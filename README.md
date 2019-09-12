common
======

[![CircleCI](https://circleci.com/gh/allenai/common/tree/master.svg?style=svg)](https://circleci.com/gh/allenai/common/tree/master)

A collection of useful utility classes and functions. Slowly on the path to deprecation.

`testkit` - Unit test classes and utilities.

`guice` - Guice-specific libraries.

`core` - Catchall collection of utilities.

Using this project as a library
------------------

`common` is published to [JCenter](https://bintray.com/bintray/jcenter) (an
alternative to Maven Central) via [BinTray](https://bintray.com/) at https://bintray.com/allenai/maven.
You will need to include [a resolver for the JCenter
repo](https://github.com/softprops/bintray-sbt#resolving-bintray-artifacts)
using the `sbt-bintray` plugin to find this artifact.

Releasing new versions
----------------------

This project releases to BinTray.  To make a release:

1. Pull the latest code on the master branch that you want to release
1. Edit `build.sbt` to remove "-SNAPSHOT" from the current version
1. Create a pull request if desired or push to master if you are only changing the version
1. Tag the release `git tag -a vX.Y.Z -m "Release X.Y.Z"` replacing X.Y.Z with the correct version
1. Push the tag back to origin `git push origin vX.Y.Z`
1. Release the build on Bintray `sbt +publish` (the "+" is required to cross-compile)
1. Verify publication [on bintray.com](https://bintray.com/allenai/maven)
1. Bump the version in `build.sbt` on master (and push!) with X.Y.Z+1-SNAPSHOT (e.g., 2.5.1
-SNAPSHOT after releasing 2.5.0)

If you make a mistake you can rollback the release with `sbt bintrayUnpublish` and retag the
 version to a different commit as necessary.

Guideline for Contributing to `common`
---------------------------

There is no strict process for contributing to `common`. However, following are some general guidelines.

### Discuss in Pull Request Code Reviews ###

If you have implemented something in a repository other than `common` and that you think could be a candidate to be migrated into `common`, ask reviewers for feedback when issuing your pull request.

### Create a GitHub Issue ###

Feel free create a GitHub issue in the `common` project to provide traceability and a forum for discussion.

### Use TODO Comments ###

While working on a task, go ahead and implement the functionality that you think would be a good fit for `common`,
and comment the implementation with a TODO suggesting it belongs in `common`. An example:

    // TODO(mygithubusername): migrate to common
    object ResourceHandling {
      type Resource = { def close(): Unit }
      def using[A](resource: => Resource)(f: Resource => A) {
        try {
          f(resource)
        finally {
          resource.close()
        }
      }
    }

If you have created a GitHub issue for the `common` candidate, it is a good idea for traceability to
reference the issue number in your TODO comment:

    // TODO(mygithubusername): migrate to common. See https://github.com/allenai/common/issues/123
    ...

### Have Two Code Reviewers to `common` Pull Requests ###

Try and always have at least two reviewers for a pull request to `common`
