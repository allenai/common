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
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.QueryBuilders

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.io.{ Source, Codec }
import scala.collection.JavaConverters._
import scala.util.{ Failure, Success }
import java.io.File
import java.nio.file.{ Files, Path }
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

  /**
   * Segment a line from a document into paragraphs.  For the files in the datastore format, this
   * is trivially done by returning the line, as they are already segmented into paragraphs.
   * @param line the line of text
   */
  def segmentByParagraph(line: String): Seq[String] = {
    if (line.trim().isEmpty()) {
      Seq.empty[String]
    } else {
      Seq[String](line)
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

  /** Index a single file into elasticsearch.
    * @param file to be indexed
    * @param bulkProcessor to communicate with the elasticsearch instance
    */
  def addFileToIndex(
    file: File,
    bulkProcessor: BulkProcessor,
    codec: Codec
  ): Unit = {
    val segmentFunction = indexType match {
      case "sentence" => defaultSegmenter.segmentTexts _
      case "paragraph" => segmentByParagraph _
      case _ => throw new IllegalStateException("unrecognized index type")
    }
    val bufSource = Source.fromFile(file, 8192)(codec)
    val lines = bufSource.getLines
    (lines flatMap { segmentFunction }).zipWithIndex.foreach {
      case (segment, segmentIndex) => {
        addSegmentToIndex(segment, file.getName, segmentIndex, bulkProcessor)
      }
    }
    var segmentIndex = 0
    for (line <- lines; segment <- defaultSegmenter.segmentTexts(line)) {
      addSegmentToIndex(segment, file.getName, segmentIndex, bulkProcessor)
      segmentIndex += 1
    }
    bufSource.close()
  }

  /** Index a file tree into the elasticSearch instance.  Divides work into nThreads*4 Futures. Each
    * future syncs on currentFile which is a logging variable, and then grabs the next file from the
    * stream if it is not empty.
    * @param fileTree file stream to be indexed
    * @return a sequence of Futures each representing the work done by a thread on this file tree.
    */
  def addTreeToIndex(fileTree: Iterator[Path], codec: Codec): Seq[Future[Unit]] = {
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
          if (!file.isDirectory && !file.isHidden) addFileToIndex(file, bulkProcessor, codec)
        })

        bulkProcessor.flush()
        bulkProcessor.awaitClose(Integer.MAX_VALUE, TimeUnit.DAYS)
        esClient.close()
      }
    }
  }

  /** Index a folder into the elasticsearch instance, following the convention of the waterloo
    * corpus. Sentences are encapsulated by <SENT> ... </SENT> tags.
    * @param indirPath path to the input directory
    */
  def addWaterlooDirectoryToIndex(indirPath: String, codec: Codec): Seq[Future[Unit]] = {
    val indir = new File(indirPath)
    for (file <- indir.listFiles; if !file.getName.startsWith(".")) yield {
      Future {
        file.setReadOnly()
        logger.debug("Now indexing: " + file.getName)
        val esClient = ElasticSearchTransportClientUtil.
          ConstructTransportClientFromESconfig(esConfig, sniffMode = true)
        val bulkProcessor: BulkProcessor = bulkProcessorUtility.
          buildDumpOnErrorBulkProcessor(esClient, dumpFolderPath)
        addWaterlooFileToIndex(file, bulkProcessor, codec)
        logger.debug("Done indexing: " + file.getName)
        bulkProcessor.flush()
        bulkProcessor.awaitClose(Integer.MAX_VALUE, TimeUnit.DAYS)
        esClient.close()
      }
    }
  }

  /** Index a file into the elasticsearch instance, following the convention of the waterloo corpus.
    * Sentences are encapsulated by <SENT> ... </SENT> tags.
    * @param inputFile path to the input directory
    * @param bulkProcessor to communicate with the elasticsearch instace
    */
  def addWaterlooFileToIndex(inputFile: File, bulkProcessor: BulkProcessor, codec: Codec): Unit = {
    indexType match {
      case "sentence" => {}
      case "paragraph" => throw new IllegalStateException("paragraph segmenting not implemented "
        + "for waterloo corpora")
      case _ => throw new IllegalStateException("unrecognized index type")
    }
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

  /** Build an index in ElasticSearch using the corpora specified in config. */
  def buildElasticSearchIndex(): Unit = {
    if (buildFromScratch) {

      // Get Transport Client.
      val esClient = ElasticSearchTransportClientUtil.ConstructTransportClientFromESconfig(esConfig)
      val createIndexRequestBuilder: CreateIndexRequestBuilder =
        esClient.admin().indices().prepareCreate(indexName)

      // Optimize settings for indexing
      createIndexRequestBuilder.setSettings(ImmutableSettings.settingsBuilder()
        .put("number_of_shards", 1)
        .put("number_of_replicas", 0))

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

    // Compile list of documents to be indexed in various formats
    val waterlooPathList: Seq[(String, String)] =
      BuildCorpusIndex.getWaterlooPathList(corpusConfigs)
    val dataStoreFileTreeList: Seq[(Path, String)] =
      BuildCorpusIndex.getDataStoreFileTreeList(corpusConfigs)
    val dataStorePathList: Seq[(Path, String)] =
      BuildCorpusIndex.getDataStoreFilePathList(corpusConfigs)

    // Index all files
    val datastorePathResults: Seq[Future[Unit]] = dataStorePathList.map {
      case (path, encoding) =>
        Future[Unit] {
          logger.debug(s"Currently indexing: ${path.getFileName}")
          val esClient =
            ElasticSearchTransportClientUtil.ConstructTransportClientFromESconfig(esConfig)
          val bulkProcessor: BulkProcessor =
            bulkProcessorUtility.buildDumpOnErrorBulkProcessor(esClient, dumpFolderPath)
          addFileToIndex(path.toFile, bulkProcessor, encoding)
          bulkProcessor.flush()
          bulkProcessor.awaitClose(Integer.MAX_VALUE, TimeUnit.DAYS)
          esClient.close()
        }
    }
    val datastoreTreeResults: Seq[Future[Unit]] = dataStoreFileTreeList flatMap {
      case (path, encoding) => addTreeToIndex(Files.walk(path).iterator().asScala, encoding)
    }
    val waterlooPathResults: Seq[Future[Unit]] = waterlooPathList flatMap {
      case (path, encoding) => addWaterlooDirectoryToIndex(path, encoding)
    }

    // combine all results into a single Future
    val results: Future[Seq[Unit]] = Future.sequence(
      datastoreTreeResults ++ datastorePathResults ++ waterlooPathResults
    )

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
}

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

  /** Compile list if file paths (for files in Waterloo Corpus format).
    * @param corpusConfigs list of all corpus configs
    * @return filtered list of waterloo format paths, paired with their encodings
    */
  def getWaterlooPathList(corpusConfigs: Seq[Config]): Seq[(String, String)] = {
    corpusConfigs.filter(corpusConfig => {
      val corpusType = corpusConfig.get[String]("corpusType")
      corpusType.isDefined && corpusType.get.equals("waterloo")
    }).map(corpusConfig => (corpusConfig[String]("directory"), corpusConfig.get[String]("encoding").
      getOrElse("UTF-8")))
  }

  /** Compile list of file trees from datastore.
    * @param corpusConfigs list of all corpus configs
    * @return filtered list of datastore directory file trees, paired with their encodings
    */
  def getDataStoreFileTreeList(corpusConfigs: Seq[Config]): Seq[(Path, String)] = {
    val datastoreCorporaConfigs = corpusConfigs.filter(corpusConfig => {
      val corpusType = corpusConfig.get[String]("corpusType")
      corpusType.isEmpty || corpusType.get.equals("datastore")
    })

    datastoreCorporaConfigs.filter(config => config.hasPath("directory") && !config.hasPath("file"))
      .map(config =>
        (
          Datastore(config.get[String]("privacy").getOrElse("private"))
          .directoryPath(
            config[String]("group"),
            config[String]("directory"),
            config[Int]("version")
          ),
            config.get[String]("encoding").getOrElse("UTF-8")
        ))
  }

  /** Compile list of file paths from datastore.
    * @param corpusConfigs list of all corpus configs
    * @return filtered list of datastore paths, paired with their encodings
    */
  def getDataStoreFilePathList(corpusConfigs: Seq[Config]): Seq[(Path, String)] = {
    val datastoreCorporaConfigs = corpusConfigs.filter(corpusConfig => {
      val corpusType = corpusConfig.get[String]("corpusType")
      corpusType.isEmpty || corpusType.get.equals("datastore")
    })

    datastoreCorporaConfigs.filter(config => config.hasPath("file")).map { config =>
      val privacy = config.get[String]("privacy").getOrElse("private")
      val path = config match {
        case fileWithDir if config.hasPath("directory") => {
          val fileString: String = fileWithDir[String]("file")
          Datastore(privacy)
            .directoryPath(
              fileWithDir[String]("group"),
              fileWithDir[String]("directory"),
              fileWithDir[Int]("version")
            )
            .resolve(fileString)
        }
        case fileWithoutDir => Datastore(privacy).filePath(
          fileWithoutDir[String]("group"),
          fileWithoutDir[String]("file"),
          fileWithoutDir[Int]("version")
        )
      }
      (path, config.get[String]("encoding").getOrElse("UTF-8"))
    }
  }
}

