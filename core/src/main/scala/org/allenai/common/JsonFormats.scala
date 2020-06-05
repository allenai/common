package org.allenai.common

import com.typesafe.config.{ Config => TypesafeConfig }
import spray.json._

import java.io.PrintWriter
import java.io.StringWriter
import scala.util.{ Failure, Success, Try }

/** Common spray.json.JsonFormats, spray.json.JsonReaders, and spray.json.JsonWriters */
object JsonFormats {

  implicit val throwableWriter: RootJsonWriter[Throwable] = new RootJsonWriter[Throwable] {

    /** Write a throwable as an object with 'message' and 'stackTrace' fields. */
    override def write(t: Throwable): JsValue = {
      Resource.using(new StringWriter) { stringWriter =>
        Resource.using(new PrintWriter(stringWriter)) { printWriter =>
          t.printStackTrace(printWriter)
          JsObject(
            "message" -> JsString(t.getMessage),
            "stackTrace" -> JsString(stringWriter.toString)
          )
        }
      }
    }
  }

  /** Handle any subclass of Throwable */
  implicit def exceptionWriter[E <: Throwable]: RootJsonWriter[E] =
    throwableWriter.asInstanceOf[RootJsonWriter[E]]

  /** Writer for an Try[T] where T has an implicit JsonWriter[T] */
  implicit def tryWriter[R: JsonWriter]: RootJsonWriter[Try[R]] = new RootJsonWriter[Try[R]] {
    override def write(responseTry: Try[R]): JsValue = {
      responseTry match {
        case Success(r) => JsObject("success" -> r.toJson)
        case Failure(t) => JsObject("failure" -> t.toJson)
      }
    }
  }

  /** Default JsonFormat for com.typesafe.config.Config */
  implicit val typesafeConfigFormat: RootJsonFormat[TypesafeConfig] =
    Config.DefaultTypesafeConfigFormat
}
