common
======

**Boss**: Michael

A collection of useful utility classes and functions.

`testkit` - Unit test classes and utilities.
`webapp` - Spray- and web-specific tools.
`core` - Catchall collection of utilities, with smaller dependency footprint than `webapp`.


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

