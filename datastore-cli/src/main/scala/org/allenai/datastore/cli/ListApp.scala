package org.allenai.datastore.cli

import org.allenai.datastore.Datastore

object ListApp extends App {
  case class Config(
    datastore: Datastore = Datastore,
    group: Option[String] = None)

  val parser = new scopt.OptionParser[Config]("scopt") {
    opt[String]('d', "datastore") action { (d, c) =>
      c.copy(datastore = Datastore(d))
    } text (s"Datastore to use. Default is ${Datastore.defaultName}")

    opt[String]('g', "group") action { (g, c) =>
      c.copy(group = Some(g))
    } text ("Group name of the objects to list")
  }

  parser.parse(args, Config()) foreach { config =>
    config.group match {
      case None =>
        config.datastore.listGroups.foreach(println)
      case Some(group) =>
        config.datastore.listGroupContents(group).foreach { l =>
          val nameSuffix = if (l.directory) "/" else ""
          println(s"${l.name}$nameSuffix\t${l.version}")
        }
    }
  }
}
