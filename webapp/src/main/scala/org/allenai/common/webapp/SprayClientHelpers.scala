package org.allenai.common.webapp

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.Http.HostConnectorSetup
import spray.can.client.{ ClientConnectionSettings, HostConnectorSettings }
import spray.http.HttpHeaders.`User-Agent`
import spray.http.{ HttpRequest, HttpResponse }

import scala.concurrent.Future
import scala.concurrent.duration._

/** Utility methods for sending HTTP requests through spray without being tripped-up by the
  * nastiness of spray's API / underlying implementation. The two methods in this object are
  * intended for use together.
  *
  * Example: Making quick GET requests with little internal buffering.
  * format: OFF
  * {{{
  *   import SprayClientHelpers._
  *
  *   // Define parameters for sending requests to the foo service.
  *   // This can be done once per web client instance as part of initialization, if all of the
  *   // fields are constant.
  *   val quickConnectorSetup = getConnectionSetup(
  *     host = "foo.com",
  *     port = 1234,
  *     connectionTimeout = 500.millis,
  *     requestTimeout = 1.second,
  *     maxConnections = 100
  *   )
  *
  *   // Define a function that requests a Foo object from the remote service and parses the
  *   // response JSON.
  *   def getFoo: Future[Foo] = sendRequest(Get("/foo"), quickConnectorSetup) { response =>
  *     response ~> unmarshal[Foo]
  *   }
  *
  *   // Completes in ~ 1 second, either with a Seq of 100 Foos or spray's RequestTimeoutException.
  *   Future.sequence(Seq.fill(100) { getFoo })
  * }}}
  * format: ON
  *
  * Example: Making slow, CPU-intensive POST requests with internal rate-limiting.
  * format: OFF
  * {{{
  *   import SprayClientHelpers._
  *
  *   val slowConnectorSetup = getConnectorSetup(
  *     host = "bar.com",
  *     port = 9876,
  *     connectionTimeout = 1.second,
  *     requestTimeout = 10.seconds,
  *     // Limit the number of in-flight requests at any one time to 4, to avoid overloading the
  *     // remote service.
  *     maxConnections = 4
  *   )
  *
  *   def fooToBar(foo: Foo): Future[Bar] = {
  *     sendRequest(Post("/bar", foo), slowConnectorSetup) { response =>
  *       response ~> unmarshal[Bar]
  *     }
  *   }
  *
  *   // Takes longer than 10 seconds! Only 4 requests will be sent over the wire at a time, and
  *   // the 10-second request timeout doesn't apply until a request gets sent out.
  *   Future.sequence(Seq.fill(16) { fooToBar(someFoo) })
  * }}}
  * format: ON
  */
object SprayClientHelpers {
  /** Send an HTTP request through spray using a dedicated `HostConnectorSetup`, and process the
    * response using the given function. This gives you much more control over how and when your
    * request is sent over the wire / when it times out than does use of `sendReceive`.
    * Caveat: this function is designed to practically prevent you from needing to worry about
    * catching `AskTimeoutExceptions` when using spray, but it's not actually possible to
    * guarantee. If you somehow send so many requests that one is internally buffered for more than
    * `Int.MaxValue.millis`, you'll see an `AskTimeoutException` thrown.
    * @param request the `HttpRequest` object to send over the wire
    * @param connectorSetup the `HostConnectorSetup` object defining connection and timeout
    * information for your request. See `getConnectionSetup` for a method of building these
    * connectors.
    * @param parseResponse a function from `HttpResponse` to a generic value `T` you want to
    * extract from your request's response
    */
  def sendRequest[T](
    request: HttpRequest,
    connectorSetup: HostConnectorSetup
  )(parseResponse: HttpResponse => T)(implicit actorSystem: ActorSystem): Future[T] = {
    import actorSystem.dispatcher

    // Spray requires a top-level timeout for sending requests into its infrastructure even
    // though it has internal timeouts built-in. We set this ridiculously high timeout here so we
    // never have to deal with uninformative `AskTimeoutExceptions`.
    implicit val askTimeout = Timeout(Int.MaxValue.millis)

    IO(Http).ask((request, connectorSetup)) map {
      case response: HttpResponse => parseResponse(response)
    }
  }

  /** Override spray's default settings for sending requests with the given timeouts.
    * @param host the name of the remote host you want to communicate with
    * @param port the port the remote host is listening on
    * @param connectionTimeout the timeout to use when establishing a remote connection to the
    * remote host
    * @param requestTimeout the timeout to use when waiting for a response from the remote host
    * @param maxConnections the maximum number of connections to the remote host that will be held
    * open at a time by the returned connector
    * @param connectorId an optional unique ID to use as the User-Agent header for requests to
    * this host. If you want to maintain multiple host connector pools to a single remote host
    * (for example, to prevent long-running requests from interfering with quick requests),
    * setting this field to different values for each will prevent spray from sharing a connector
    * between the different types of requests.
    */
  def getConnectionSetup(
    host: String,
    port: Int,
    connectionTimeout: FiniteDuration,
    requestTimeout: FiniteDuration,
    maxConnections: Int,
    connectorId: Option[String] = None
  )(implicit system: ActorSystem): HostConnectorSetup = {
    val clientConnectionSettings = ClientConnectionSettings(system).copy(
      requestTimeout = requestTimeout,
      // Amount of time an idle HTTP connection will be held open before being closed.
      idleTimeout = Duration.Inf,
      connectingTimeout = connectionTimeout,
      userAgentHeader = connectorId map { `User-Agent`(_) }
    )

    HostConnectorSetup(
      host = host,
      port = port,
      settings = Some(
        HostConnectorSettings(system).copy(
          // Amount of time one of spray's HostConnector actors will sit idle before terminating
          // itself.
          idleTimeout = Duration.Inf,
          maxConnections = maxConnections,
          connectionSettings = clientConnectionSettings
        )
      )
    )
  }
}
