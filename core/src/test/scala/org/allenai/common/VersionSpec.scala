package org.allenai.common

import org.allenai.common.JsonFormats._
import org.allenai.common.testkit.UnitSpec

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.{ Try, Success, Failure }

class GitVersionSpec extends UnitSpec {
  "GitVersion" should "find the correct GitHub project URL (ssh)" in {
    val version = GitVersion.create("gitSha", 1234, Seq("https://github.com/schmmd/parsers.git",
        "git@github.com:allenai/common.git"))
    assert(version.repoUrl === Some("http://github.com/allenai/common"))
  }

  "GitVersion" should "find the correct GitHub project URL (https)" in {
    val version = GitVersion.create("gitSha", 1234, Seq("https://github.com/allenai/parsers.git",
        "git@github.com:schmmd/common.git"))
    assert(version.repoUrl === Some("http://github.com/allenai/parsers"))
  }

  "GitVersion" should "find the correct GitHub commit URL" in {
    val version = GitVersion.create("e0d972e185bd12b94dedd38834fea150a68f064e", 1234,
        Seq("https://github.com/allenai/parsers.git", "git@github.com:schmmd/common.git"))
    assert(version.commitUrl ===
        Some("http://github.com/allenai/parsers/commit/e0d972e185bd12b94dedd38834fea150a68f064e"))
  }
}
