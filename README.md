common
======

**Boss**: Michael

A collection of useful utility classes and functions.

`testkit` - Unit test classes and utilities.

`webapp` - Spray- and web-specific tools.

`core` - Catchall collection of utilities, with smaller dependency footprint than `webapp`.

Releasing new versions
---------------------------

This project releases to Maven Central rather than to our own repository. To do this, you need a bit of setup.

 1. You need the signing keys to publish software with. You can find them in the `ai2-secure` bucket in S3 under the key `Sonatype Key Pair.zip`. Copy that file to `~/.sbt/gpg/` and extract it there.
 2. You need the passphrase for that key pair. It's defined as an array, which is a little weird, and goes into another location in `~/.sbt`. The line defining it is in `passwords.txt` in the `ai2-secure` bucket. Copy that line into `~/.sbt/0.13/allenai.sbt` (or into some other `.sbt` if you like).
 3. To use the passphrase, we have to enable the `sbt-pgp` plugin. Put the following line into `~/.sbt/0.13/plugins/gpg.sbt`: `addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")`
 4. We also need credentials to the sonatype repository. We get those with the following line in `~/.sbt/0.13/sonatypt.sbt`: `credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", "allenai-role", "<password>")`. You find this password in the same `password.txt` file from above.

Now, you need to register your GPG key.

1. Start SBT in the common project
2. At the SBT prompt, type:

   ```bash
   > pgp-cmd send-key [TAB]
   Paul Allen Institute for Artificial Intelligence <account>
   abcdefg
   ```
 
   When you hit [TAB], SBT should print out the available key and its ID on the second line (in the example above, `abcdefg`. Enter the id:
 
   ```bash
   > pgp-cmd send-key abcdefg hkp://keyserver.ubuntu.com [ENTER]
   ```

With this, you should be ready to run `sbt release` on the common project. When you do, it will upload the build artifacts to a staging repository on http://oss.sonatype.org. When it's done, you have to go there and first close, and then release, the staging repository. That initiates the upload to Maven Central, which will take about 10 minutes.

 1. Go to http://oss.sonatype.org.
 2. Log in with username `allenai-role`, and the password from the `password.txt` file. This is the same password you used in step 4 above.
 3. Click "staging repositories" on the left.
 4. Use the search bar at the top right to search for "allenai".
 5. Find your staging repository and confirm that it has the contents you expect. Then, select it and click "close". Closing takes a few minutes. Then you can see how the closing process went under "Activity". It sends an email to `dev-role@allenai.org` when it's done.
 6. When it is done, select the repository again and hit "close".

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

