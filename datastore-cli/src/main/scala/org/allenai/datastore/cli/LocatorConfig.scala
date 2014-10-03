package org.allenai.datastore.cli

import org.allenai.datastore.Datastore

trait LocatorConfig {
  val group: String
  val name: String
  val version: Int
  val datastore: Datastore

  def locator = datastore.Locator(group, name, version)
}
