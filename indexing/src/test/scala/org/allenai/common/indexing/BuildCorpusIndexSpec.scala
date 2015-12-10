package org.allenai.common.indexing

import org.allenai.common.testkit.UnitSpec

import com.typesafe.config.{ ConfigFactory, Config }
import java.nio.file.Paths

class BuildCorpusIndexSpec extends UnitSpec {

  val dir1 = "/test/path/dir1/"
  val file1 = "test/file1"

  /** Test an given parse result against expected results.  We need to test like this when we're
    * accessing something from the Datastore, because we have to check a suffix on the file path,
    * instead of just checking for object equality.
    */
  def expectParse(
    parsedConfig: ParsedConfig,
    pathSuffix: String,
    isDirectory: Boolean,
    encoding: String,
    documentFormat: String
  ) {
    parsedConfig.path.toString should endWith(pathSuffix)
    parsedConfig.isDirectory should be(isDirectory)
    parsedConfig.encoding should be(encoding)
    parsedConfig.documentFormat should be(documentFormat)
  }

  "parseCorpusConfig" should "parse a local directory" in {
    val corpusConfig = ConfigFactory.parseString(s"""{
      |pathIsLocal: true
      |documentFormat: "waterloo"
      |directory: "${dir1}"
      |}""".stripMargin)
    val expectedParse = ParsedConfig(Paths.get(dir1), true, "UTF-8", "waterloo")
    BuildCorpusIndex.parseCorpusConfig(corpusConfig) should be(expectedParse)
  }

  it should "parse a local file with directory" in {
    val corpusConfig = ConfigFactory.parseString(s"""{
      |pathIsLocal: true
      |documentFormat: "waterloo"
      |directory: "${dir1}"
      |file: "${file1}"
      |}""".stripMargin)
    val expectedConfig = ParsedConfig(Paths.get(dir1, file1), false, "UTF-8", "waterloo")
    BuildCorpusIndex.parseCorpusConfig(corpusConfig) should be(expectedConfig)
  }

  it should "parse a local file without a directory" in {
    val corpusConfig = ConfigFactory.parseString(s"""{
      |pathIsLocal: true
      |documentFormat: "waterloo"
      |file: "${file1}"
      |}""".stripMargin)
    val expectedConfig = ParsedConfig(Paths.get(file1), false, "UTF-8", "waterloo")
    BuildCorpusIndex.parseCorpusConfig(corpusConfig) should be(expectedConfig)
  }

  it should "parse a datastore file" in {
    val corpusConfig = ConfigFactory.parseString(s"""{
      |group: "org.allenai.corpora.wikipedia"
      |file: "simple_wikipedia_first_few_articles.txt"
      |version: 1
      |privacy: "public"
      |}""".stripMargin)
    val parsed = BuildCorpusIndex.parseCorpusConfig(corpusConfig)
    expectParse(
      parsed,
      "org.allenai.datastore/public/org.allenai.corpora.wikipedia/" +
        "simple_wikipedia_first_few_articles-v1.txt",
      false,
      "UTF-8",
      "plain text"
    )
  }

  it should "parse a datastore directory" in {
    val corpusConfig = ConfigFactory.parseString(s"""{
      |group: "org.allenai.aristo.corpora.derivative"
      |directory: "Barrons-4thGrade.sentences"
      |version: 1
      |}""".stripMargin)
    val parsed = BuildCorpusIndex.parseCorpusConfig(corpusConfig)
    expectParse(
      parsed,
      "org.allenai.datastore/private/org.allenai.aristo.corpora.derivative/" +
        "Barrons-4thGrade.sentences-d1",
      true,
      "UTF-8",
      "plain text"
    )
  }

  it should "parse a datastore file with a directory" in {
    val corpusConfig = ConfigFactory.parseString(s"""{
      |group: "org.allenai.aristo.corpora.derivative"
      |directory: "Barrons-4thGrade.sentences"
      |file: "Barrons-5.sentences.txt"
      |encoding: "fake encoding"
      |version: 1
      |}""".stripMargin)
    val parsed = BuildCorpusIndex.parseCorpusConfig(corpusConfig)
    expectParse(
      parsed,
      "org.allenai.datastore/private/org.allenai.aristo.corpora.derivative/" +
        "Barrons-4thGrade.sentences-d1/Barrons-5.sentences.txt",
      false,
      "fake encoding",
      "plain text"
    )
  }
}
