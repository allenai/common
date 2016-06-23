package org.allenai.common.indexing

import com.typesafe.config.ConfigFactory

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

    val originalConfig =
      rootConfig.getConfig(s"org.allenai.common.indexing.${config.indexConfigName}").resolve()
    val configOverrides = config.configOverrideFile map { f => ConfigFactory.parseFile(f) }

    /** Get merged Config object from applying requested overrides to original config. */
    val buildIndexConfig = configOverrides match {
      case Some(overrides) => overrides.withFallback(originalConfig)
      case None => originalConfig
    }

    new BuildCorpusIndex(buildIndexConfig).buildElasticSearchIndex()
  })

}
