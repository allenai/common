package org.allenai.common

import org.allenai.common.testkit.UnitSpec

import spray.json._

import java.net.URLClassLoader
import java.nio.file.Paths

class GitVersionSpec extends UnitSpec {
  "create" should "find the correct GitHub project URL (ssh)" in {
    val version = GitVersion.create(
      "gitSha",
      1234,
      Seq(
        "https://github.com/schmmd/parsers.git",
        "git@github.com:allenai/common.git"
      )
    )
    version.repoUrl shouldBe Some("http://github.com/allenai/common")
  }

  it should "find the correct GitHub project URL (https)" in {
    val version = GitVersion.create(
      "gitSha",
      1234,
      Seq(
        "https://github.com/allenai/ari-datastore.git",
        "git@github.com:schmmd/common.git"
      )
    )
    version.repoUrl shouldBe Some("http://github.com/allenai/ari-datastore")
  }

  it should "find the correct GitHub commit URL" in {
    val version = GitVersion.create(
      "e0d972e185bd12b94dedd38834fea150a68f064e",
      1234,
      Seq("https://github.com/allenai/parsers.git", "git@github.com:schmmd/common.git")
    )
    version.commitUrl shouldBe
      Some("http://github.com/allenai/parsers/commit/e0d972e185bd12b94dedd38834fea150a68f064e")
  }
}

class VersionSpec extends UnitSpec {
  "Version" should "be backwards compatible for reading" in {
    val json = """{
      "git":"0144af4325992689cf5fd6d0e3c2d744b25935d6",
      "artifact":"2014.07.21-0-SNAPSHOT","commitDate":1412094251000
    }"""
    json.parseJson.convertTo[Version] shouldBe
      Version(
        GitVersion("0144af4325992689cf5fd6d0e3c2d744b25935d6", 1412094251000L, None),
        "2014.07.21-0-SNAPSHOT",
        None
      )
  }

  "fromResources" should "find common-core's resources" in {
    // No asserts; this will throw an exception if it's unfound.
    Version.fromResources("org.allenai.common", "common-core")
  }

  it should "find a resource using a class loader" in {
    val expectedVersion = Version(
      GitVersion("sha123", 123456789L, None),
      "1.0.0",
      None
    )
    val classpath = Paths.get("src/test/resources/fakejar").toAbsolutePath.toUri.toURL
    val version = Version.fromResources(
      "org.fakeorg",
      "project-name",
      new URLClassLoader(Array(classpath))
    )
    version shouldBe expectedVersion
  }
}
