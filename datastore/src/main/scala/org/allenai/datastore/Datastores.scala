package org.allenai.datastore

import java.io.InputStream
import java.nio.file.{ Files, Path }

/** A collection of convenience methods for accessing the datastore from a class
  *
  * Inherit this trait in your class to get easy access to the private and the public datastore. By
  * default, the group name for all items is the name of the package the class lives in, but you can
  * override it by overriding the datastoreGroup member.
  */
trait Datastores {
  /** The name of the group used in the other methods of this trait. Override this if you need
    * another group name.
    */
  val datastoreGroup: String = this.getClass.getPackage.getName

  val privateDatastore = PrivateDatastore
  val publicDatastore = Datastore

  def privateFile(name: String, version: Int): Path =
    privateDatastore.filePath(datastoreGroup, name, version)
  def publicFile(name: String, version: Int): Path =
    publicDatastore.filePath(datastoreGroup, name, version)

  def privateStream(name: String, version: Int): InputStream =
    Files.newInputStream(privateFile(name, version))
  def publicStream(name: String, version: Int): InputStream =
    Files.newInputStream(publicFile(name, version))

  def privateDirectory(name: String, version: Int): Path =
    privateDatastore.directoryPath(datastoreGroup, name, version)
  def publicDirectory(name: String, version: Int): Path =
    publicDatastore.directoryPath(datastoreGroup, name, version)
}
