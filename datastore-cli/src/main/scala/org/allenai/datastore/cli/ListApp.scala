package org.allenai.datastore.cli

import org.allenai.datastore.Datastore

object ListApp extends App {
  case class Config(datastore: Datastore = Datastore)

  val parser = new scopt.OptionParser[Config]("scopt") {
    opt[String]('d', "datastore") action { (d, c) =>
      c.copy(datastore = new Datastore(d))
    } text (s"Datastore to use. Default is ${Datastore.name}")
  }

  parser.parse(args, Config()) foreach { config =>
    config.datastore.listGroups.foreach(println)
  }
}
