package org.allenai.common.webapp

import org.allenai.common.testkit.ActorSpec

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport

import scala.concurrent.{ Await, Future, TimeoutException }
import scala.concurrent.duration._

class SprayClientHelpersSpec extends ActorSpec(ActorSystem("SprayClientHelpersSpec"))
    with SprayJsonSupport {

  import system.dispatcher

  // Set up a dummy server to test sending requests and parsing responses.
  val server = new DummyServer()
  val testHost = "localhost"
  val testPort = 6000
  val connectionTimeout = 250.millis
  val requestTimeout = 500.millis

  val connector = SprayClientHelpers.getConnectionSetup(
    testHost,
    testPort,
    connectionTimeout,
    requestTimeout,
    1
  )

  override def beforeAll(): Unit = Await.result(server.start(testPort), 2.seconds)

  "SprayClientHelpers" should "support receiving raw string responses" in {
    val request = SprayClientHelpers.sendRequest(Get("/hello"), connector) { response =>
      response ~> unmarshal[String]
    }

    Await.result(request, 2 * requestTimeout) shouldBe "hi!"
  }

  it should "support transforming unmarshalled values within its response-parser" in {
    val request = SprayClientHelpers.sendRequest(Get("/addOne?number=5"), connector) { response =>
      (response ~> unmarshal[String]).toInt - 7
    }

    Await.result(request, 2 * requestTimeout) shouldBe (5 + 1 - 7)
  }

  it should "support unmarshalling to more than just strings" in {
    val message = "hello json"
    val request = SprayClientHelpers.sendRequest(Post("/", Ping(message)), connector) { response =>
      response ~> unmarshal[Pong]
    }

    Await.result(request, 2 * requestTimeout) shouldBe Pong(message)
  }

  it should "allow for a variable number of requests to run in parallel" in {
    val connector2 = SprayClientHelpers.getConnectionSetup(
      testHost,
      testPort,
      connectionTimeout,
      requestTimeout,
      2
    )

    val requests2 = Seq.fill(2) {
      SprayClientHelpers.sendRequest(Get(s"/sleep/${requestTimeout.toMillis}"), connector2)(identity)
    }

    // All we care about here is that the two requests run in parallel.
    Await.result(Future.sequence(requests2), requestTimeout * 1.1)

    val requests3 = Seq.fill(3) {
      SprayClientHelpers.sendRequest(Get(s"/sleep/${requestTimeout.toMillis}"), connector2)(identity)
    }

    // Waiting with the same timeout on 3 requests should fail, indicating there are only 2
    // connectors.
    intercept[TimeoutException] {
      Await.result(Future.sequence(requests3), requestTimeout * 1.1)
    }
  }

  it should "support sending requests to a single host via separate connectors" in {
    val connectorA = SprayClientHelpers.getConnectionSetup(
      testHost,
      testPort,
      connectionTimeout,
      requestTimeout,
      1,
      Some("A")
    )

    val connectorB = SprayClientHelpers.getConnectionSetup(
      testHost,
      testPort,
      connectionTimeout,
      requestTimeout,
      1,
      Some("B")
    )

    val requests = Seq(
      SprayClientHelpers.sendRequest(Get(s"/sleep/${requestTimeout.toMillis}"), connectorA)(identity),
      SprayClientHelpers.sendRequest(Get(s"/sleep/${requestTimeout.toMillis}"), connectorB)(identity)
    )

    // Again, we only care that the two requests run in parallel even though each host setup only
    // has one connector.
    Await.result(Future.sequence(requests), requestTimeout * 1.1)
  }
}
