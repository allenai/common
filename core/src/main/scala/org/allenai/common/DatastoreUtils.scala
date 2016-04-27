package org.allenai.common

import org.allenai.datastore.Datastore

import com.typesafe.config.{ Config => TypesafeConfig }

import java.io.File

import scala.io.{ BufferedSource, Source }

/** Various convenient utiltiies for accessing the Datastore. */
object DatastoreUtils extends Logging {
  /** Get a datastore file as a buffered Source. Caller is responsible for closing this stream. */
  def getDatastoreFileAsSource(
    datastoreName: String,
    group: String,
    name: String,
    version: Int
  ): BufferedSource = {
    logger.debug(s"Loading file from $datastoreName datastore: $group/$name-v$version")
    val file = Datastore(datastoreName).filePath(group, name, version).toFile
    Source.fromFile(file)
  }

  /** Get a datastore file as a buffered Source. Caller is responsible for closing this stream. */
  def getDatastoreFileAsSource(config: TypesafeConfig): BufferedSource = {
    val datastoreName = config.getString("datastore")
    val group = config.getString("group")
    val name = config.getString("name")
    val version = config.getInt("version")
    getDatastoreFileAsSource(datastoreName, group, name, version)
  }

  /** Get a datastore directory as a folder. */
  def getDatastoreDirectoryAsFolder(
    datastoreName: String,
    group: String,
    name: String,
    version: Int
  ): File = {
    logger.debug(s"Loading directory from $datastoreName datastore: $group/$name-v$version")
    Datastore(datastoreName).directoryPath(group, name, version).toFile
  }

  /** Get a datastore directory as a folder. */
  def getDatastoreDirectoryAsFolder(config: TypesafeConfig): File = {
    val datastoreName = config.getString("datastore")
    val group = config.getString("group")
    val name = config.getString("name")
    val version = config.getInt("version")
    getDatastoreDirectoryAsFolder(datastoreName, group, name, version)
  }
}
