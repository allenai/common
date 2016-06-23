package org.allenai.common.indexing

import org.allenai.common.Config._
import org.allenai.common.Logging
import org.allenai.common.ParIterator._
import org.allenai.datastore.Datastore
import org.allenai.nlpstack.segment.defaultSegmenter

import com.typesafe.config.{ ConfigRenderOptions, ConfigFactory, ConfigObject, Config }
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.QueryBuilders

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.io.{ Source, Codec }
import scala.collection.JavaConverters._
import scala.util.{ Failure, Success }
import java.io.File
import java.nio.file.{ Files, Path, Paths }
import java.util.concurrent.TimeUnit

/** CLI to build an Elastic Search index on Aristo corpora.
  * In order to build the index, you need to have elasticsearch running.
  * Download latest version of elasticsearch, go to the 'bin' folder and run it:
  * ./elasticsearch
  * Refer http://joelabrahamsson.com/elasticsearch-101/ to get started.
  * Takes in Config object containing corpus and other information necessary to build the index.
  */
class BuildCorpusIndex(config: Config) extends Logging {

  /** Get Index Name and Index Type. */
  val esConfig: Config = config[Config]("elasticSearch")
  val indexName: String = esConfig[String]("indexName")
  val indexType: String = esConfig[String]("indexType")

  val buildFromScratch = config.get[Boolean]("buildIndexOptions.buildFromScratch").getOrElse(true)

  val nThreads = config.get[Int]("buildIndexOptions.nThreads") getOrElse {
    Runtime.getRuntime.availableProcessors()
  }

  /** On failure, dump serialized requests to this path. */
  val dumpFolderPath = config[String]("buildIndexOptions.dumpFolder")
  val bulkProcessorUtility = new BulkProcessorUtility

  /** Regex used to split sentences in waterloo corpus. */
  val sentenceSplitRegex = """</?SENT>""".r.unanchored

  /** Build an index in ElasticSearch using the corpora specified in config. */
  def buildElasticSearchIndex(): Unit = {
    if (buildFromScratch) {

      // Get Transport Client.
      val esClient = ElasticSearchTransportClientUtil.ConstructTransportClientFromESconfig(esConfig)
      val createIndexRequestBuilder: CreateIndexRequestBuilder =
        esClient.admin().indices().prepareCreate(indexName)

      createIndexRequestBuilder.setSettings(Settings.settingsBuilder())

      val indexSetting = esConfig.get[ConfigObject]("setting").getOrElse(ConfigFactory.empty.root)
      val indexMapping = esConfig.get[ConfigObject]("mapping").getOrElse(ConfigFactory.empty.root)

      if (!indexSetting.isEmpty) {
        // Add custom settings to index
        val indexSettingString = indexSetting.render(ConfigRenderOptions.concise())
        createIndexRequestBuilder.setSettings(indexSettingString)
      }

      if (!indexMapping.isEmpty) {
        // Add mapping to index
        val indexMappingString = indexMapping.render(ConfigRenderOptions.concise())
        createIndexRequestBuilder.addMapping(indexType, indexMappingString)
      }

      createIndexRequestBuilder.execute().actionGet()
      esClient.close()
    }

    val corpusConfigs = config.get[Seq[Config]]("corpora").getOrElse(Seq.empty[Config])
    val parsedConfigs = corpusConfigs.map(parseCorpusConfig)

    val results: Future[Seq[Unit]] = Future.sequence(parsedConfigs.flatMap(corpus => {
      if (corpus.isDirectory) {
        val iterator = Files.walk(corpus.path).iterator().asScala
        addTreeToIndex(iterator, corpus.encoding, corpus.documentFormat)
      } else {
        addTreeToIndex(Seq(corpus.path).iterator, corpus.encoding, corpus.documentFormat)
      }
    }))

    results onComplete {
      case Success(l) =>
        logger.debug(s"Done creating index ${indexName}, type: ${indexType}!")
      case Failure(l) =>
        logger.error(s"Unable to create index: ${l.printStackTrace()}")
    }

    Await.result(results, Duration.Inf)

    val failedRequests = bulkProcessorUtility.getFailedRequests()

    if (failedRequests.length > 0) {

      // Retry failed requests
      logger.debug("Retrying failed requests")

      val esClient = ElasticSearchTransportClientUtil.ConstructTransportClientFromESconfig(esConfig)
      for (bulkRequest <- failedRequests; request <- bulkRequest.requests().asScala) {
        BuildCorpusIndex.indexWithoutDuplicate(
          request.asInstanceOf[IndexRequest], esClient, indexName
        )
      }
      esClient.close
    } else {
      logger.debug("No failed requests")
    }
  }

  /** Index a file tree into the elasticSearch instance.  Divides work into nThreads*4 Futures. Each
    * future syncs on currentFile which is a logging variable, and then grabs the next file from the
    * stream if it is not empty.
    * @param fileTree file stream to be indexed
    * @return a sequence of Futures each representing the work done by a thread on this file tree.
    */
  def addTreeToIndex(
    fileTree: Iterator[Path],
    codec: Codec,
    documentFormat: String
  ): Seq[Future[Unit]] = {
    for (i <- 0 until nThreads * 4) yield {
      Future {
        val esClient =
          ElasticSearchTransportClientUtil.ConstructTransportClientFromESconfig(esConfig)
        val bulkProcessor: BulkProcessor =
          bulkProcessorUtility.buildDumpOnErrorBulkProcessor(esClient, dumpFolderPath)

        // Implicit conversion here to ParIteratorEnrichment
        fileTree parForeach (path => {
          val file = path.toFile
          // ignore .DS_STORE and any other hidden surprises that should not be indexed
          if (!file.isDirectory && !file.isHidden) {
            addFileToIndex(file, bulkProcessor, codec, documentFormat)
          }
        })

        bulkProcessor.flush()
        bulkProcessor.awaitClose(Integer.MAX_VALUE, TimeUnit.DAYS)
        esClient.close()
      }
    }
  }

  /** Index a single file into elasticsearch.
    * @param file to be indexed
    * @param bulkProcessor to communicate with the elasticsearch instance
    */
  def addFileToIndex(
    file: File,
    bulkProcessor: BulkProcessor,
    codec: Codec,
    documentFormat: String
  ): Unit = {
    if (documentFormat == "waterloo") {
      addWaterlooFileToIndex(file, bulkProcessor, codec)
    } else {
      val segments = segmentFile(file, codec, documentFormat)
      segments.zipWithIndex.foreach {
        case (segment, segmentIndex) => {
          addSegmentToIndex(segment, file.getName, segmentIndex, bulkProcessor)
        }
      }
    }
  }

  /** Index a file into the elasticsearch instance, following the convention of the waterloo corpus.
    * Sentences are encapsulated by <SENT> ... </SENT> tags.
    * @param inputFile path to the input directory
    * @param bulkProcessor to communicate with the elasticsearch instace
    */
  def addWaterlooFileToIndex(inputFile: File, bulkProcessor: BulkProcessor, codec: Codec): Unit = {
    var filePositionCounter = 0

    def segmentFunction(segment: String): Unit = {
      addSegmentToIndex(segment, inputFile.getName, filePositionCounter, bulkProcessor)
      filePositionCounter += 1
    }
    ParsingUtils.splitOnTag(
      inputFile = inputFile,
      splitString = "DOC",
      splitRegex = sentenceSplitRegex,
      segmentFunction = segmentFunction,
      bufferSize = 16384,
      codec
    )
  }

  def segmentFile(file: File, codec: Codec, documentFormat: String): Iterator[String] = {
    documentFormat match {
      case "plain text" => segmentPlainTextFile(file, codec)
      case "barrons" => getSegmentsFromDocument(new BarronsDocumentReader(file, codec).read())
      case "simple wikipedia" => segmentWikipediaFile(file, codec)
      case "waterloo" => throw new IllegalStateException("you shouldn't have gotten here")
      case _ => throw new IllegalStateException("Unrecognized document format")
    }
  }

  def getSegmentsFromDocument(document: SegmentedDocument): Iterator[String] = {
    val segments = document.getSegmentsOfType(indexType)
    segments.map(_.getTextSegments.mkString(" ")).iterator
  }

  def segmentPlainTextFile(file: File, codec: Codec): Iterator[String] = {
    if (indexType != "sentence") {
      throw new IllegalStateException("plain text can only be segmented into sentences")
    }
    val bufSource = Source.fromFile(file, 8192)(codec)
    val lines = bufSource.getLines
    (lines flatMap { defaultSegmenter.segmentTexts })
  }

  def segmentWikipediaFile(file: File, codec: Codec): Iterator[String] = {
    indexType match {
      case "sentence" => segmentPlainTextFile(file, codec)
      case "paragraph" => {
        val bufSource = Source.fromFile(file, 8192)(codec)
        val lines = bufSource.getLines
        lines.flatMap(line => if (line.trim.isEmpty) Seq[String]() else Seq[String](line))
      }
      case _ => throw new IllegalStateException("unrecognized index type")
    }
  }

  /** Index a single segment into elasticsearch.
    * @param segment to be indexed
    * @param source name of source for reference
    * @param segmentIndex index of segment in file (for deduplication)
    * @param bulkProcessor to communicate with the elasticsearch instance
    */
  def addSegmentToIndex(
    segment: String,
    source: String,
    segmentIndex: Int,
    bulkProcessor: BulkProcessor
  ): Unit = {
    val request = new IndexRequest(indexName, indexType).source(jsonBuilder().startObject()
      .field("text", segment.trim)
      .field("source", source + "_" + segmentIndex.toString)
      .endObject())
    bulkProcessor.add(request)
  }

  /** Take the config for a corpus, resolve paths, and return a simple object containing information
    * about the corpus.
    */
  def parseCorpusConfig(corpusConfig: Config): ParsedConfig = {
    val documentFormat = corpusConfig.get[String]("documentFormat").getOrElse("plain text")
    val encoding = corpusConfig.get[String]("encoding").getOrElse("UTF-8")
    // We could be a little smarter at detecting whether the intent was a local path, but this will
    // do for now.
    val pathIsLocal = corpusConfig.get[Boolean]("pathIsLocal").getOrElse(false)
    val (path, isDirectory) = pathIsLocal match {
      case true => getLocalPathFromConfig(corpusConfig)
      case false => getDatastorePathFromConfig(corpusConfig)
    }
    ParsedConfig(path, isDirectory, encoding, documentFormat)
  }

  def getLocalPathFromConfig(corpusConfig: Config): (Path, Boolean) = {
    val directory = corpusConfig.get[String]("directory")
    val file = corpusConfig.get[String]("file")
    file match {
      case Some(f) => {
        directory match {
          case Some(d) => (Paths.get(d, f), false)
          case None => (Paths.get(f), false)
        }
      }
      case None => (Paths.get(directory.get), true)
    }
  }

  def getDatastorePathFromConfig(corpusConfig: Config): (Path, Boolean) = {
    val directory = corpusConfig.get[String]("directory")
    val file = corpusConfig.get[String]("file")
    val privacy = corpusConfig.get[String]("privacy").getOrElse("private")
    val group = corpusConfig[String]("group")
    val version = corpusConfig[Int]("version")
    file match {
      case Some(f) => (getFileFromDatastore(privacy, group, directory, f, version), false)
      case None => (getDirectoryFromDatastore(privacy, group, directory.get, version), true)
    }
  }

  def getFileFromDatastore(
    privacy: String,
    group: String,
    directory: Option[String],
    file: String,
    version: Int
  ): Path = {
    directory match {
      case Some(d) => Datastore(privacy).directoryPath(group, d, version).resolve(file)
      case None => Datastore(privacy).filePath(group, file, version)
    }
  }

  def getDirectoryFromDatastore(
    privacy: String,
    group: String,
    directory: String,
    version: Int
  ): Path = {
    Datastore(privacy).directoryPath(group, directory, version)
  }
}

case class ParsedConfig(path: Path, isDirectory: Boolean, encoding: String, documentFormat: String)

object BuildCorpusIndex {
  /** Execute a given index request if the document is not already in the index. */
  def indexWithoutDuplicate(
    request: IndexRequest,
    esClient: TransportClient,
    indexName: String
  ): Unit = {
    val source = request.sourceAsMap().asScala("source")
    val result = esClient.prepareSearch(indexName)
      .setQuery(QueryBuilders.termQuery("source", source))
      .execute()
      .actionGet()
    if (result.getHits.getTotalHits == 0) {
      esClient.index(request).actionGet()
    }
  }
}
