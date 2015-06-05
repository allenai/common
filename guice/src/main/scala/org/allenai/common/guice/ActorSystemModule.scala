package org.allenai.common.guice

import akka.actor.ActorSystem
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

/** Module that binds an ActorSystem and its associated ExecutionContext.
  * @param bindingName optional name to create the bindings under
  * @param actorSystem the actor system to bind
  */
class ActorSystemModule(bindingName: Option[String] = None)(implicit actorSystem: ActorSystem)
    extends ScalaModule {
  override def configure(): Unit = {
    bindingName match {
      case Some(name) => {
        bind[ActorSystem].annotatedWithName(name).toInstance(actorSystem)
        bind[ExecutionContext].annotatedWithName(name).toInstance(actorSystem.dispatcher)
      }
      case None => {
        bind[ActorSystem].toInstance(actorSystem)
        bind[ExecutionContext].toInstance(actorSystem.dispatcher)
      }
    }
  }
}
