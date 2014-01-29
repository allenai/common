[![Build Status](https://api.travis-ci.com/allenai/common.png?token=iR6Jn6hFD9RbunxYtisP)](https://magnum.travis-ci.com/allenai/common)

common
======

A collection of useful utility classes and functions.

Guideline for Contributing to `common`
---------------------------

There is no strict process for contributing to `common`. However, following are some general guidelines.

### Discuss in Pull Request Code Reviews ###

If you are unsure that something you've implemented belongs in `common`, ask for feedback when issuing
your pull request.

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

    // TODO(markschaake): migrate to common [allenai/common#123]
	...

