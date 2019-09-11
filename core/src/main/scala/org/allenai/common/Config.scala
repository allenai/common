package org.allenai.common

import com.typesafe.config.{Config => TypesafeConfig, _}
import spray.json._

import java.net.URI
import scala.collection.JavaConverters._
import scala.concurrent.duration._

/** Import to provide enhancements via implicit class conversion for making working
  * with [[com.typesafe.config.Config]] more Scala-friendly (no nulls!).
  *
  * Also provides a `spray.json.RootJsonFormat[Config]`.
  */
object Config {

  class TypesafeConfigFormat(pretty: Boolean) extends RootJsonFormat[TypesafeConfig] {
    val ParseOptions = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON)
    val RenderOptions = ConfigRenderOptions.concise().setFormatted(pretty).setJson(true)

    override def read(jsValue: JsValue): TypesafeConfig = jsValue match {
      case obj: JsObject => ConfigFactory.parseString(obj.compactPrint, ParseOptions)
      case _ => deserializationError("Expected JsObject for Config deserialization")
    }

    override def write(config: TypesafeConfig): JsValue =
      JsonParser(config.root.render(RenderOptions))
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

    /** Generates a new ConfigReader[A] from the T value extracted
      *
      * @tparam A the type of the new ConfigReader
      * @param f function that transforms a T into an A
      */
    def map[A](f: T => A): ConfigReader[A] = {
      val self = this
      new ConfigReader[A] {
        override def read(config: TypesafeConfig, key: String): A = f(self.read(config, key))
      }
    }
  }

  object ConfigReader {

    /** Factory for creating a new ConfigReader[T] type class instance */
    def apply[T](f: (TypesafeConfig, String) => T): ConfigReader[T] = new ConfigReader[T] {
      def read(config: TypesafeConfig, key: String): T = f(config, key)
    }

    // ConfigReader wrappers for built-in Typesafe Config extractors that may return null
    implicit val stringReader = apply[String] { (config, key) =>
      config.getString(key)
    }
    implicit val intReader = apply[Int] { (config, key) =>
      config.getInt(key)
    }
    implicit val longReader = apply[Long] { (config, key) =>
      config.getLong(key)
    }
    implicit val doubleReader = apply[Double] { (config, key) =>
      config.getDouble(key)
    }
    implicit val boolReader = apply[Boolean] { (config, key) =>
      config.getBoolean(key)
    }
    implicit val configValueReader = apply[ConfigValue] { (config, key) =>
      config.getValue(key)
    }

    implicit val stringListReader = apply[Seq[String]] { (config, key) =>
      config.getStringList(key).asScala
    }
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
    implicit val configValueListReader = apply[Seq[ConfigValue]] { (config, key) =>
      config.getList(key).asScala.toSeq
    }

    implicit val configObjReader = apply[ConfigObject] { (config, key) =>
      config.getObject(key)
    }
    implicit val typesafeConfigReader = apply[TypesafeConfig] { (config, key) =>
      config.getConfig(key)
    }
    implicit val typesafeConfigListReader = apply[Seq[TypesafeConfig]] { (config, key) =>
      config.getConfigList(key).asScala.toSeq
    }

    // Other common types that could occur in config files

    /** In addition to com.typesafe.config.ConfigException,
      * will potentially throw java.net.URISyntaxException
      */
    implicit val uriReader: ConfigReader[URI] = stringReader map { URI.create(_) }

    // convert config object to a JsValue
    // this is useful for doing two-step conversion from config value to some class that already has
    // a JsFormat available (and therefore the user doesn't have to also define a ConfigReader)
    // Note: any exceptions due to JSON parse (such as DeserializationException) will not be caught.
    val jsonReader: ConfigReader[JsValue] = configObjReader map { _.toConfig.toJson }
  }

  /** Adds Scala-friendly methods to a [[com.typesafe.config.Config]] instance:
    *
    * Examples:
    *
    * {{{
    * import org.allenai.common.Config._
    *
    * val config = ConfigFactory.load()
    * val requiredConfigValue: String = config[String]("required.key")
    * val optionalConfigValue: Option[URI] = config.get[URI]("optional.key")
    * }}}
    */
  implicit class EnhancedConfig(config: TypesafeConfig) {
    private def optional[T](f: => T) =
      try {
        Some(f)
      } catch {
        case e: ConfigException.Missing => None
      }

    /** Required value extraction.
      * @throws com.typesafe.config.ConfigException
      */
    def apply[T](key: String)(implicit reader: ConfigReader[T]): T = reader.read(config, key)

    /** Optional value extraction.
      *
      * Catches any com.typesafe.config.ConfigException.Missing exceptions and converts to None.
      *
      * @throws com.typesafe.config.ConfigException
      */
    def get[T](key: String)(implicit reader: ConfigReader[T]): Option[T] =
      optional { apply[T](key) }

    /** Required JSON parse.
      *
      * @throws com.typesafe.config.ConfigException
      */
    def fromJson[T](key: String)(implicit reader: JsonReader[T]): T = {
      ConfigReader.jsonReader.read(config, key).convertTo[T]
    }

    /** Optional JSON parse */
    def getFromJson[T](key: String)(implicit reader: JsonReader[T]): Option[T] =
      optional { fromJson[T](key) }

    def getScalaDuration(key: String, timeUnit: TimeUnit): Option[Duration] =
      optional { Duration(config.getDuration(key, timeUnit), timeUnit) }
  }
}
