package org.allenai.common.webapp

import okhttp3._
import spray.json._

import java.net.URLEncoder

/** A simple case class representing one of the "blue links" from a Bing API query.
  */
case class BingResult(
  query: String,
  pos: Int,
  id: String,
  url: String,
  title: String,
  description: String
)

/** A client that wraps calls to the Bing API.
  * @param apiKey the Azure key
  */
class BingClient(apiKey: String) {
  import scala.concurrent.ExecutionContext.Implicits.global

  val client = new OkHttpClient()

  def closeConnection() = {
    val req = new Request.Builder()
      .url("https://api.cognitive.microsoft.com")
      .header("Connection", "close")
      .get()
      .build();
    client.newCall(req).execute()
  }

  /** The new v5 Bing API returns URLs as bing redirects, with the original url in the query
    * string as the r= parameter. This extracts that parameter.
    *
    * @param redirectUrl the Bing redirect url
    * @return the original url
    */
  def extractUrlFromBingRedirect(redirectUrl: String): Option[String] = {
    new java.net.URI(redirectUrl)
      .getQuery()
      .split('&')
      // Find the query param that looks like r=....
      .flatMap("(?s)^r=(.*)$".r.findFirstMatchIn)
      // There should be exactly one, but use .headOption to be safe
      .headOption
      .map(_.group(1))
  }

  /** Synchronously issues a query to the Bing API.
    * @param query what to search for
    * @param responseFilter ...
    * @param top number of desired results, defaults to 10
    * @return all valid results as a sequence
    */
  def query(query: String, responseFilter: String = "webpages", top: Int = 10): Seq[BingResult] = {
    // Create the URI representing the query
    val encodedQuery = "%27" + URLEncoder.encode(query, "UTF-8") + "%27"
    val filter = if (responseFilter.isEmpty) "" else s"&responseFilter=${responseFilter}"

    val uri = s"https://api.cognitive.microsoft.com/bing/v5.0/" +
      s"search?q=${encodedQuery}&count=${top}${filter}"

    val request = new Request.Builder()
      .url(uri)
      .header("Ocp-Apim-Subscription-Key", apiKey)
      .build()

    val response = client.newCall(request).execute()
    val rawData = response.body.string
    val json = JsonParser(rawData).asJsObject

    // The results we want are an array at json["webPages"]["value"].
    val rawResults = json
      .getFields("webPages").head.asJsObject
      .getFields("value").head.asInstanceOf[JsArray]

    // Extract the results from the JsArray and map them to our case class.
    rawResults match {
      case JsArray(elements) => elements.zipWithIndex.flatMap {
        case (jsValue, pos) =>
          val jsMap = jsValue.asJsObject.fields
          for {
            id <- getString("id", jsMap)
            redirectUrl <- getString("url", jsMap)
            url = extractUrlFromBingRedirect(redirectUrl).getOrElse(redirectUrl)
            title <- getString("name", jsMap)
            description <- getString("snippet", jsMap)
          } yield BingResult(query, pos, id, url, title, description)
      }
    }
  }

  /** A helper function to get a string out of a JsObject
    * @param key name of the field
    * @param jsMap the map of Json fields
    * @return the string, or None if unavailable
    */
  private def getString(key: String, jsMap: Map[String, JsValue]): Option[String] = {
    jsMap.get(key).flatMap {
      case JsString(s) => Some(s)
      case _ => None
    }
  }
}
