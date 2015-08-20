common
======

**Boss**: Michael

A collection of useful utility classes and functions.

`testkit` - Unit test classes and utilities.

`webapp` - Spray- and web-specific tools.

`guice` - Guice-specific libraries.

`core` - Catchall collection of utilities, with smaller dependency footprint than `webapp`.

Using this project as a library
------------------

`common` is published to [JCenter](https://bintray.com/bintray/jcenter) (an
alternative to Maven Central) via [BinTray](https://bintray.com/) at https://bintray.com/allenai/maven.  If you have
the latest version of our [core plugins](https://github.com/allenai/sbt-plugins) the [BinTray resolver will be included
by default](https://github.com/allenai/sbt-plugins/blob/master/src/main/scala/org/allenai/plugins/CoreRepositories.scala#L24).
Otherwise you will need to include [a resolver for the JCenter
repo](https://github.com/softprops/bintray-sbt#resolving-bintray-artifacts)
using the `sbt-bintray` plugin to find this artifact.

Releasing new versions
----------------------

This project releases to BinTray.  To make a release, follow our standard
release process.

1.  Make sure you are on a branch of the main repository other than `master`.
**You cannot use a branch of your fork for releases.**  If you do, your tag
will not make it to the main repository.
2.  Set the upstream repository (`git push --set-upstream origin $branch_name`).
3.  Kick of the release with `sbt release`.
4.  Create a pull request and make sure Semaphore builds it OK.
5.  When the pull request is merged, Semaphore will kick off a build and ultimately publish the artifact.
6.  You can verify that it was published by [looking on BinTray.com](https://bintray.com/allenai/maven)!

You are done!


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

