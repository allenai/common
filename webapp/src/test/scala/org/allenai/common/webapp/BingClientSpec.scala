package org.allenai.common.webapp

import org.allenai.common.testkit.UnitSpec

import java.util.NoSuchElementException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

class BingClientSpec extends UnitSpec {
  // If the Azure auth key isn't defined, just skip these tests.
  val apiKey = try {
    Some(sys.env("AZURE_AUTH_KEY"))
  } catch {
    case e: NoSuchElementException => None
  }

  val bingClient = apiKey.map(new BingClient(_))

  "bingClient" should "execute a single query" in {
    if (bingClient.isDefined) {
      val results = bingClient.get.query("aardvark")
      assert(results.nonEmpty)
    } else {
      cancel("AZURE_AUTH_KEY not defined, skipping test")
    }
  }
}
