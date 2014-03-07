package org.allenai.common.testkit

import akka.actor._

object ActorSpecSpec {
  case class Message(value: String, replies: Int = 1)

  class EchoActor extends Actor {
    override def receive = {
      case Message("ping", replies) => (1 to replies) foreach { _ => sender ! "pong" }
      case Message(x, replies) => (1 to replies) foreach { _ => sender ! x }
    }
  }
}

class ActorSpecSpec(actorSystem: ActorSystem) extends ActorSpec(actorSystem) {
  import ActorSpecSpec._

  def this() = this(ActorSystem("ActorSpecSpec"))

  "ActorSpec" should "test for expected message" in {
    val echo = system.actorOf(Props[EchoActor])
    echo ! Message("ping")

    // expectMsg is a helper provided by the Akka TestKit
    expectMsg("pong")
  }

  it should "test for N expected messages" in {
    val echo = system.actorOf(Props[EchoActor])
    echo ! Message("hi", 10)

    // receiveN is a helper provided by the Akka TestKit
    receiveN(10)
  }
}
