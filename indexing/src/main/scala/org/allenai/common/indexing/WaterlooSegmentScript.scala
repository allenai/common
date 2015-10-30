package org.allenai.common.indexing

import org.allenai.common.Logging
import org.allenai.datastore.Datastore
import org.allenai.nlpstack.segment.defaultSegmenter

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.io.{ Codec, Source }
import java.io.{ FileWriter, BufferedWriter, File }

/** Script used to segment waterloo corpus on a sentence level.
  * Splits docs based on <DOC> ... </DOC> tags, determines whether the doc is in "English" by
  * counting the fraction of stop words, and throws out the doc if it is not. Sentence segments the
  * doc using nlp stack, wraps each sentence in <SENT> ... </SENT> tags, and then rewrites the
  * entire doc to file.
  */
object WaterlooSegmentScript extends App with Logging {

  val englishThreshold = 0.2

  val rootConfig = ConfigFactory.systemProperties.withFallback(ConfigFactory.load)

  val config = ConfigFactory.parseResources(getClass, "application.conf").resolve().
    getConfig("org.allenai.common.indexing.waterloo-lucene")

  // Get Index Name and Index Type
  val esConfig = config.getConfig("elasticSearch")
  val indexName = esConfig.getString("indexName")
  val splitString = "DOC"
  val splitRegex = """</?DOC>""".r.unanchored

  val stopWordsConfig = config.getConfig("stoplist")

  val stopWords = Source.fromFile(Datastore("public").
    filePath(stopWordsConfig.getString("group"), stopWordsConfig.getString("name"),
      stopWordsConfig.getInt("version")).toFile)
    .getLines().toVector.toSet

  val corpusConfig = config.getConfig("CorpusIOConfig")
  val indirPath = corpusConfig.getString("inputFolder")
  val outdirPath = corpusConfig.getString("outputFolder")

  segmentDirectory(indirPath, outdirPath)

  def segmentDirectory(inputDirectoryName: String, outputDirectoryName: String): Unit = {

    val indir = new File(inputDirectoryName)
    val outdir = new File(outputDirectoryName)

    if (!outdir.exists()) outdir.mkdir()

    val results: Seq[Future[Unit]] = for (
      file <- indir.listFiles;
      if !file.getName.startsWith(".")
    ) yield {
      Future {
        logger.debug("Now segmenting: " + file.getName)
        segmentIntoDocs(file, new File(outdir.getAbsolutePath + "/" + file.getName))
        logger.debug("Done segmenting: " + file.getName)
      }
    }
    Await.result(Future.sequence(results), Duration.Inf)
    logger.debug("Done segmenting!")
  }

  def segmentIntoDocs(inputFile: File, outputFile: File): Unit = {

    outputFile.delete()
    val writer = new BufferedWriter(new FileWriter(outputFile, false))

    ParsingUtils.splitOnTag(inputFile, splitString, splitRegex, dealWithDocHelper, 8192, Codec.UTF8)

    writer.flush()
    writer.close()

    def dealWithDocHelper(input: String): Unit = {
      dealWithDoc(input, writer, inputFile.getName)
    }
  }

  def dealWithDoc(input: String, bufferedWriter: BufferedWriter, source: String): Unit = {
    if (!input.trim.equals("") && isEnglish(input)) {
      bufferedWriter.write("<DOC>")
      val sentences = defaultSegmenter.segmentTexts(input).
        map(sentence => s"<SENT>$sentence</SENT>")
      sentences.foreach(s => bufferedWriter.write(s))
      bufferedWriter.write("</DOC>")
    }
  }

  def isEnglish(input: String): Boolean = {
    val arr = input.split(" ")
    val total = arr.length
    val count = arr.foldLeft(0)((x: Int, y: String) => x + (if (stopWords.contains(y)) 1 else 0))
      .toDouble
    count / total > englishThreshold
  }
}
