package org.allenai.common.testkit

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

/** Trait providing helpers for dealing with [[scala.concurrent.Future]]s duruing tests
  */
trait FutureHelpers { self: AllenAiBaseSpec =>

  /** Default Timout for awaiting results from futures */
  def defaultTimeout = 5.seconds

  /** Block for result with the defaultTimeout */
  def await[A](f: => Future[A]): A = Await.result(f, defaultTimeout)

}
