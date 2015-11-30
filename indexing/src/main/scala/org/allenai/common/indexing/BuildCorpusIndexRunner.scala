package org.allenai.common.indexing

import com.typesafe.config.{ Config, ConfigFactory }

import java.io.File

/** Indexing main object. Configuration specified in indexing.conf in org.allenai.common.indexing.
  * See common/Readme for details.
  */
object BuildCorpusIndexRunner extends App {

  case class IndexConfig(indexConfigName: String = null, configOverrideFile: Option[File] = None)

  val parser = new scopt.OptionParser[IndexConfig]("BuildCorpusIndexRunner") {
    head("BuildCorpusIndexRunner")
    note("See common/Readme for details on how to use this.\n")
    help("help") text ("prints this usage text")
    opt[String]("index-name") required () valueName ("<string>") action { (x, c) =>
      c.copy(indexConfigName = x)
    } text ("name of configuration to use")
    opt[File]("config-overrides-file") valueName ("<file>") action { (x, c) =>
      c.copy(configOverrideFile = Some(x))
    } text ("Config file with overrides if any")
  }

  parser.parse(args, IndexConfig()).foreach(config => {
    val rootConfig = ConfigFactory.parseResources(getClass, "indexing.conf").resolve()
    val configOverrides = config.configOverrideFile map { f => ConfigFactory.parseFile(f) }
    new BuildCorpusIndex(rootConfig.getConfig(s"org.allenai.common.indexing.${config.indexConfigName}")
      .resolve(), configOverrides).buildElasticSearchIndex()
  })

}
