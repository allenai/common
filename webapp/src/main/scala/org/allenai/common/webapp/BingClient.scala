package org.allenai.common.webapp

import okhttp3._
import spray.json._

import scala.concurrent.Future

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

  val credentials = okhttp3.Credentials.basic(apiKey, apiKey)
  val client = new OkHttpClient()

  def closeConnection() = {
    val req = new Request.Builder()
      .url("https://api.datamarket.azure.com")
      .header("Connection", "close")
      .get()
      .build();
    client.newCall(req).execute()
  }

  /** Synchronously issues a query to the Bing API.
    * @param query what to search for
    * @param sourceType The type of result to search for, defaults to `Web`. For valid values, see
    * (https://msdn.microsoft.com/en-us/library/dd250895.aspx).
    * @param top number of desired results, defaults to 10
    * @return all valid results as a sequence
    */
  def query(query: String, sourceType: String = "Web", top: Int = 10): Seq[BingResult] = {
    // Create the URI representing the query
    val encodedQuery = "%27" + URLEncoder.encode(query, "UTF-8") + "%27"
    val baseUrl = "https://api.datamarket.azure.com/Bing/SearchWeb/" + sourceType
    val uri = baseUrl + "?Query=" + encodedQuery + "&$top=" + top.toString + "&$format=json"

    val request = new Request.Builder()
      .url(uri)
      .header("Authorization", credentials)
      .build()

    val response = client.newCall(request).execute()
    val rawData = response.body.string
    val json = JsonParser(rawData).asJsObject

    // The results we want are an array at json["d"]["results"].
    val rawResults = json
      .getFields("d").head.asJsObject
      .getFields("results").head.asInstanceOf[JsArray]

    // Extract the results from the JsArray and map them to our case class.
    rawResults match {
      case JsArray(elements) => elements.zipWithIndex.flatMap {
        case (jsValue, pos) =>
          val jsMap = jsValue.asJsObject.fields
          for {
            id <- getString("ID", jsMap)
            url <- getString("Url", jsMap)
            title <- getString("Title", jsMap)
            description <- getString("Description", jsMap)
          } yield BingResult(query, pos, id, url, title, description)
      }
    }
  }

  /** Execute several queries in parallel, running the supplied callback on each individual result.
    * @param queries sequence of queries to submit
    * @param top number of desired results for each query
    * @return a sequence of futures mapping each result
    */
  def bulkQuery(queries: Seq[String], top: Int = 10) = queries.map {
    q => Future { query(q, top = top) }
  }

  /** A helper function to get a string out of a JsObject
    * @param key name of the field
    * @param jsMap the map of Json fields
    * @return the string, or None if unavailable
    */
  private def getString(key: String, jsMap: Map[String, JsValue]) = jsMap.get(key).flatMap {
    case JsString(s) => Some(s)
    case _ => None
  }
}
