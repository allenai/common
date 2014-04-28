package org.allenai.common

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.{ Config => TypesafeConfig }
import com.typesafe.config.{ ConfigParseOptions, ConfigSyntax }
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/** Import to provide enhancements via implicit class conversion for making working
  * with [[com.typesafe.config.Config]] more Scala-friendly (no nulls!).
  *
  * Also provides a Spray JSON RootJsonFormat[Config].
  */
object Config {

  class TypesafeConfigFormat(pretty: Boolean) extends RootJsonFormat[TypesafeConfig] {
    val ParseOptions = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON)
    val RenderOptions = ConfigRenderOptions.concise().setFormatted(pretty).setJson(true)

    override def read(jsValue: JsValue): TypesafeConfig = jsValue match {
      case obj: JsObject => ConfigFactory.parseString(obj.compactPrint, ParseOptions)
      case _ => deserializationError("Expected JsObject for Config deserialization")
    }

    override def write(config: TypesafeConfig): JsValue = JsonParser(config.root.render(RenderOptions))
  }

  /** Renders JSON formatted */
  object PrettyTypesafeConfigFormat extends TypesafeConfigFormat(pretty = true)

  /** Renders JSON on compact (no whitespace) format */
  object ConciseTypesafeConfigFormat extends TypesafeConfigFormat(pretty = false)

  implicit val DefaultTypesafeConfigFormat = ConciseTypesafeConfigFormat

  /** Type class that defines method for reading a value of type T from a Typesafe Config key */
  trait ConfigReader[T] {
    /** Returns Some[T] if key is present, None if key is missing */
    def read(config: TypesafeConfig, key: String): T
  }

  object ConfigReader {
    /** Factory for creating a new ConfigReader[T] type class instance */
    def apply[T](f: (TypesafeConfig, String) => T) = new ConfigReader[T] {
      def read(config: TypesafeConfig, key: String) = f(config, key)
    }

    // ConfigReader wrappers for built-in Typesafe Config extractors that may return null
    implicit val stringReader = apply[String] { (config, key) => config.getString(key) }
    implicit val intReader = apply[Int] { (config, key) => config.getInt(key) }
    implicit val longReader = apply[Long] { (config, key) => config.getLong(key) }
    implicit val doubleReader = apply[Double] { (config, key) => config.getDouble(key) }
    implicit val boolReader = apply[Boolean] { (config, key) => config.getBoolean(key) }

    implicit val stringListReader = apply[Seq[String]] { (config, key) => config.getStringList(key).asScala }
    implicit val intListReader = apply[Seq[Int]] { (config, key) =>
      config.getIntList(key).asScala.toList.map(_.intValue)
    }
    implicit val longListReader = apply[Seq[Long]] { (config, key) =>
      config.getLongList(key).asScala.toList.map(_.longValue)
    }
    implicit val boolListReader = apply[Seq[Boolean]] { (config, key) =>
      config.getBooleanList(key).asScala.toList.map(_.booleanValue)
    }
    implicit val doubleListReader = apply[Seq[Double]] { (config, key) =>
      config.getDoubleList(key).asScala.toList.map(_.doubleValue)
    }
  }

  implicit class EnhancedConfig(config: TypesafeConfig) {
    private def optional[T](f: => T) = try {
      Some(f)
    } catch {
      case e: ConfigException.Missing => None
    }

    def get[T](key: String)(implicit reader: ConfigReader[T]): Option[T] = optional { reader.read(config, key) }

    def getScalaDuration(key: String, timeUnit: TimeUnit): Option[Duration] = optional {
      Duration(config.getDuration(key, timeUnit), timeUnit)
    }
  }
}
