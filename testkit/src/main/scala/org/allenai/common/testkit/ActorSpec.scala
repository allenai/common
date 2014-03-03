package org.allenai.common.testkit

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit

import org.scalatest._

abstract class ActorSpec(actorSystem: ActorSystem)
    extends TestKit(actorSystem)
    with AllenAiBaseSpec
    with FutureHelpers
