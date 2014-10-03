package org.allenai.datastore.cli

import org.allenai.datastore.Datastore

/** A lot of the configs we're using contain information to make a Datastore Locator object. This
  * trait puts that idea into one place.
  */
trait LocatorConfig {
  val group: String
  val name: String
  val version: Int
  val datastore: Datastore

  def locator = datastore.Locator(group, name, version)
}
