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

import java.net.URL

import scala.concurrent.Future
import scala.concurrent.duration._

object SprayClientHelpers {
  /** Send an HTTP request through spray using a dedicated `HostConnectorSetup`, and process the
    * response using the given function. This gives you much more control over how and when your
    * request is sent over the wire / when it times out than does use of `sendReceive`.
    * @param request the `HttpRequest` object to send over the wire
    * @param connectorSetup the `HostConnectorSetup` object defining connection and timeout
    * information for your request
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
    * @param url the URL of the remote host to communicate with
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
    url: URL,
    connectionTimeout: FiniteDuration,
    requestTimeout: FiniteDuration,
    maxConnections: Int,
    connectorId: Option[String] = None
  )(implicit system: ActorSystem): HostConnectorSetup = {
    val clientConnectionSettings = ClientConnectionSettings(system).copy(
      requestTimeout = requestTimeout,
      idleTimeout = requestTimeout * 2,
      connectingTimeout = connectionTimeout,
      userAgentHeader = connectorId map { `User-Agent`(_) }
    )

    HostConnectorSetup(
      host = url.getHost,
      port = url.getPort,
      settings = Some(
        HostConnectorSettings(system).copy(
          idleTimeout = requestTimeout * 2,
          maxConnections = maxConnections,
          connectionSettings = clientConnectionSettings
        )
      )
    )
  }
}
