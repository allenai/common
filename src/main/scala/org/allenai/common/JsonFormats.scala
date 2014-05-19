package org.allenai.common

import com.typesafe.config.{ Config => TypesafeConfig }
import spray.json.SerializationException
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.PrintWriter
import java.io.StringWriter
import scala.util.{ Try, Success, Failure }

/** Common [[spray.json.JsonFormat]]s, [[spray.json.JsonReader]]s, and [[spray.json.JsonWriter]]s */
object JsonFormats {

  implicit val throwableWriter: RootJsonWriter[Throwable] = new RootJsonWriter[Throwable] {

    /** Write a throwable as an object with 'message' and 'stackTrace' fields. */
    override def write(t: Throwable): JsValue = {
      Resource.using(new StringWriter) { stringWriter =>
        Resource.using(new PrintWriter(stringWriter)) { printWriter =>
          t.printStackTrace(printWriter)
          JsObject(
            "message" -> JsString(t.getMessage),
            "stackTrace" -> JsString(stringWriter.toString))
        }
      }
    }
  }

  /** Handle any subclass of Throwable */
  implicit def exceptionWriter[E <: Throwable] = throwableWriter.asInstanceOf[RootJsonWriter[E]]

  /** Writer for an Try[T] where T has an implicit JsonWriter[T] */
  implicit def tryWriter[R : JsonWriter]: RootJsonWriter[Try[R]] = new RootJsonWriter[Try[R]] {
    override def write(responseTry: Try[R]) = {
      responseTry match {
        case Success(r) => JsObject("success" -> r.toJson)
        case Failure(t) => JsObject("failure" -> t.toJson)
      }
    }
  }

  /** Default JsonFormat for com.typesafe.config.Config */
  implicit val typesafeConfigFormat: RootJsonFormat[TypesafeConfig] = Config.DefaultTypesafeConfigFormat
}
