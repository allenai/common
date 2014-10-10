package org.allenai.datastore.cli

object DatastoreCli extends App {
  // TODO: Use scopt's support for commands instead?
  args.headOption match {
    case Some("upload") | Some("up") =>
      UploadApp.main(args.drop(1))
    case Some("download") | Some("down") =>
      DownloadApp.main(args.drop(1))
    case Some("list") | Some("ls") =>
      ListApp.main(args.drop(1))
    case Some("wipeCache") =>
      WipeCacheApp.main(args.drop(1))
    case Some("web") | Some("url") =>
      UrlApp.main(args.drop(1))
    case _ =>
      println(s"Usage: ${this.getClass.getName} <command>")
      println("<command> is one of {upload, download, list, url, wipeCache}")
  }
}
