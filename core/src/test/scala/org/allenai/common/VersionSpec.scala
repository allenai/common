package org.allenai.common

import org.allenai.common.JsonFormats._
import org.allenai.common.testkit.UnitSpec

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.{ Try, Success, Failure }

class VersionSpec extends UnitSpec {
  "Version" should "find the correct GitHub project URL (ssh)" in {
    val version = Version("gitSha", 1234, Seq("https://github.com/schmmd/parsers.git", "git@github.com:allenai/common.git"), "artifactVersion")
    assert(version.githubProjectUrl("allenai") === Some("http://github.com/allenai/common"))
  }

  "Version" should "find the correct GitHub project URL (https)" in {
    val version = Version("gitSha", 1234, Seq("https://github.com/allenai/parsers.git", "git@github.com:schmmd/common.git"), "artifactVersion")
    assert(version.githubProjectUrl("allenai") === Some("http://github.com/allenai/parsers"))
  }

  "Version" should "find the correct GitHub commit URL" in {
    val version = Version("e0d972e185bd12b94dedd38834fea150a68f064e", 1234, Seq("https://github.com/allenai/parsers.git", "git@github.com:schmmd/common.git"), "artifactVersion")
    assert(version.githubCommitUrl("allenai") === Some("http://github.com/allenai/parsers/commit/e0d972e185bd12b94dedd38834fea150a68f064e"))
  }
}