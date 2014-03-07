package org.allenai.common.testkit

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit

import org.scalatest._

/** Base class for Akka Actor integration specs
  *
  * By extending [[akka.testkit.TestKit]] and [[akka.testkit.ImplicitSender]],
  * we get many helpers for testing Actors against a live actor system.
  *
  * For more information on Akka TestKit, see:
  * http://doc.akka.io/docs/akka/2.2.4/scala/testing.html#Asynchronous_Integration_Testing_with_TestKit
  */
abstract class ActorSpec(actorSystem: ActorSystem)
    extends TestKit(actorSystem)
    with AllenAiBaseSpec
    with ImplicitSender
    with FutureHelpers
    with BeforeAndAfterAll {

  /** Ensure the actor system is shutdown once all tests are complete or have failed */
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}
