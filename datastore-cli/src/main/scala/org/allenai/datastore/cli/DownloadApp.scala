package org.allenai.datastore.cli

import org.allenai.datastore.Datastore

object DownloadApp extends App {
  case class Config(
    assumeFile: Boolean = false,
    assumeDirectory: Boolean = false,
    group: String = null,
    name: String = null,
    version: Int = -1,
    datastore: Datastore = Datastore)

  val parser = new scopt.OptionParser[Config]("scopt") {
    opt[Boolean]('f', "assumeFile") action { (f, c) =>
      c.copy(assumeFile = f)
    } text ("Assumes that the object in the datastore is a file.")

    opt[Boolean]('d', "assumeDirectory") action { (d, c) =>
      c.copy(assumeDirectory = d)
    } text ("Assumes that the object in the datastore is a directory.")

    checkConfig { c =>
      if (c.assumeDirectory && c.assumeFile) {
        failure("You can't specify both assumeDirectory and assumeFile")
      } else {
        success
      }
    }

    note("If you specify neither assumeDirectory nor assumeFile, the tool will autodetect " +
      "whether the object in the datastore is a file or a directory.")

    opt[String]('g', "group") required () action { (g, c) =>
      c.copy(group = g)
    } text ("Group name of the object in the datastore")

    opt[String]('n', "name") required () action { (n, c) =>
      c.copy(name = n)
    } text ("Name of the object in the datastore")

    opt[Int]('v', "version") required () action { (v, c) =>
      c.copy(version = v)
    } text ("Version number of the object in the datastore")

    opt[String]('d', "datastore") action { (d, c) =>
      c.copy(datastore = Datastore(d))
    } text (s"Datastore to use. Default is ${Datastore.defaultName}")

    help("help")
  }

  parser.parse(args, Config()) foreach { config =>
    val datastore = config.datastore
    val locator = datastore.Locator(config.group, config.name, config.version)
    val fileMode = if (config.assumeDirectory) {
      false
    } else if (config.assumeFile) {
      true
    } else {
      !datastore.directoryExists(locator)
    }

    println(if (fileMode) datastore.filePath(locator) else datastore.directoryPath(locator))
  }
}
