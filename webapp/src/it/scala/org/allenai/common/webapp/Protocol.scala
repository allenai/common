package org.allenai.common.webapp

import spray.json.DefaultJsonProtocol._

case class Ping(message: String)
object Ping {
  implicit val pingJsonFormat = jsonFormat1(Ping.apply)
}

case class Pong(message: String)
object Pong {
  implicit val pongJsonFormat = jsonFormat1(Pong.apply)
}
