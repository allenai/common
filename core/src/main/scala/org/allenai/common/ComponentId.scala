package org.allenai.common

/** A specific version of a component in AI2. This fully-describes a running system - it contains
  * enough information to re-deploy the version that generated the ComponentId instance.
  * @param name a human-readable name of the component
  * @param version the version of the component that was running
  */
case class ComponentId(name: String, version: Version)

object ComponentId {
  import spray.json.DefaultJsonProtocol._
  implicit val componentIdJsonFormat = jsonFormat2(ComponentId.apply)
}
