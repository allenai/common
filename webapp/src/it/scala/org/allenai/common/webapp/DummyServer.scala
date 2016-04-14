package org.allenai.common.webapp

import akka.actor.ActorSystem
import akka.pattern.after
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.routing.{ Route, SimpleRoutingApp }

import scala.concurrent.Future
import scala.concurrent.duration._

class DummyServer(implicit actorSystem: ActorSystem) extends SimpleRoutingApp with SprayJsonSupport {
  import actorSystem.dispatcher

  // format: OFF
  def route: Route = {
    get {
      path("hello") { complete("hi!") } ~
      path("addOne") {
        parameter('number.as[Int]) { number => complete((number + 1).toString()) }
      } ~
      path("sleep" / Segment) { sleepMillis =>
        complete {
          after(sleepMillis.toInt.millis, actorSystem.scheduler) {
            Future.successful("done")
          }
        }
      }
    } ~
    post {
      entity(as[Ping]) { ping => complete(Pong(ping.message)) }
    }
  }
  // format: ON

  def start(port: Int): Future[Http.Bound] = startServer("0.0.0.0", port)(route)
}
