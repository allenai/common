package org.allenai.common.indexing

import com.typesafe.config.ConfigFactory

/** Indexing main object. Configuration specified in indexing.conf in org.allenai.common.indexing.
 *  See common/Readme for details.
 */
object BuildCorpusIndexRunner extends App {

  case class IndexConfig(indexConfigName: String = null)

  val parser = new scopt.OptionParser[IndexConfig]("BuildCorpusIndexRunner") {
    head("BuildCorpusIndexRunner")
    note("See common/Readme for details on how to use this.\n")
    help("help") text ("prints this usage text")
    arg[String]("<indexConfigName>...") required () action { (x, c) =>
      c.copy(indexConfigName = x)
    } text ("name of configuration to use")
  }

  parser.parse(args, IndexConfig()).foreach(config => {
    val rootConfig = ConfigFactory.parseResources(getClass, "indexing.conf").resolve()
    new BuildCorpusIndex(rootConfig.getConfig(s"org.allenai.common.indexing.${config.indexConfigName}")
      .resolve()).buildElasticSearchIndex()
  })

}
