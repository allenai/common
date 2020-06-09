package org.allenai.common.guice

import org.allenai.common.Compat.JavaConverters._
import org.allenai.common.Logging
import org.allenai.common.Config._

import com.google.inject.name.Named
import com.typesafe.config.{
  Config,
  ConfigException,
  ConfigFactory,
  ConfigObject,
  ConfigUtil,
  ConfigValueType
}
import net.codingwell.scalaguice.ScalaModule

import scala.util.Try

/** Parent class for modules which use a typesafe config for values. This automatically binds all
  * configuration values within a given Config instance, along with defaults from an optional
  * bundled config file. Each binding is annotated with `@Named(configPath)` to differentiate
  * multiple bindings for a single primitive type.
  *
  * This will bind config (HOCON) value types boolean, number, string, list, and object. Boolean and
  * string entries are bound to the corresponding scala type. Numbers are bound to Double if they're
  * floating point, and are bound to Int, Long, and Double if they're integral. Lists are bound to
  * Seq[Config], since HOCON allows mixed list types. All object-valued keys are also bound as
  * Config instances.
  *
  * The default config filename is looked for in the implementing class's path, using the resource
  * name `module.conf`.  For example, if the implementing module is org.allenai.foobar.MyModule,
  * `module.conf` should be in `src/main/resources/org/allenai/foobar`. `defaultConfig` provides the
  * filename, if you want to change it from the default.
  *
  * Example config and bindings:
  *
  * Config file -
  * format: OFF
  * {{{
  * stringValue = "foo"
  * someObject = {
  *   propNumber = 123
  *   propBool = true
  * }
  * }}}
  *
  * Injected Scala class -
  * {{{
  * class Injected @Inject() (
  *   @Named("stringValue") foo: String,
  *   @Named("someObject.propBool") boolValue: Boolean,
  *   @Named("someObject.propNumber") integralValue: Int,
  *   someOtherParameter: ScalaClass,
  *   @Named("someObject.propNumber") numbersCanBeDoubles: Double
  * )
  * }}}
  * format: ON
  * @param config the runtime config to use containing all values to bind
  */
class ConfigModule(config: Config) extends ScalaModule with Logging {

  /** The actual config to bind. */
  private lazy val fullConfig = {
    val resolvedConfig = config.withFallback(defaultConfig).resolve()
    bindingPrefix map { resolvedConfig.atPath } getOrElse { resolvedConfig }
  }

  /** An optional filename pointing to a file containing default config values.
    * Optional to allow for use-cases in which we want config to be fully-specified by users,
    * without losing the nice utility of using this module or introducing code smell by trying to
    * load a dummy config file.
    */
  def configName: Option[String] = None

  /** If overridden, the namespace prefix that is prepended to all binding key names. This is
    * used as a path prefix for all config values; so if the prefix is `Some("foo")` and the config
    * key is "one.two", the final binding will be for @Named("foo.one.two").
    *
    * This is useful if you're providing a module within a library, and want to have your clients be
    * able to pass Config overrides without having to worry about prefixing them properly.
    */
  def bindingPrefix: Option[String] = None

  /** The config to use as a fallback. This is where keys will be looked up if they aren't present
    * in the provided config.
    */
  def defaultConfig: Config =
    configName map { name =>
      ConfigFactory.parseResources(getClass, name)
    } getOrElse ConfigFactory.empty

  /** Configure method for implementing classes to override if they wish to create additional
    * bindings, or bindings based on config values.
    * @param config the fully-initilized config object
    */
  def configureWithConfig(config: Config): Unit = {}

  /** Binds the config provided in the constructor, plus any default config found, and calls
    * configureWithConfig with the resultant config object.
    */
  final override def configure(): Unit = {
    bindConfig()
    configureWithConfig(fullConfig)
  }

  /** Internal helper to bind the config key `key` to the given type `T`. */
  private def bindConfigKey[T](
    key: String
  )(implicit manifest: Manifest[T], configReader: ConfigReader[T]): Unit = {
    try {
      fullConfig.get[T](key) match {
        case Some(value) =>
          bind[T].annotatedWithName(key).toInstance(value)
          bind[Option[T]].annotatedWithName(key).toInstance(Some(value))
        case None =>
          addError(
            s"Config in ${getClass.getSimpleName} missing key '$key' with expected type " +
              s"'${manifest.runtimeClass.getSimpleName}'"
          )
      }
    } catch {
      case _: ConfigException.WrongType =>
        addError(
          s"Config in ${getClass.getSimpleName} has bad type for key '$key'; expected " +
            s"value of type '${manifest.runtimeClass.getSimpleName}'"
        )
    }
  }

  /** Recursively binds the given config object, located at the given path. */
  private def bindConfigObject(config: ConfigObject, pathElements: Seq[String]): Unit = {
    for (entry <- config.entrySet.asScala) {
      val key = entry.getKey
      val fullPathElements = pathElements :+ key
      val fullPath = ConfigUtil.joinPath(fullPathElements.asJava)
      val value = entry.getValue
      logger.debug(s"Binding key $fullPath to $value")
      value.valueType match {
        case ConfigValueType.BOOLEAN =>
          bindConfigKey[Boolean](fullPath)
        case ConfigValueType.NUMBER =>
          value.unwrapped match {
            case _: java.lang.Integer =>
              // Bind both floating-point & integral versions, since the config system treats
              // whole-valued numbers as integers.
              bindConfigKey[Int](fullPath)
              bindConfigKey[Long](fullPath)
              bindConfigKey[Double](fullPath)
            case _: java.lang.Long =>
              bindConfigKey[Long](fullPath)
              bindConfigKey[Double](fullPath)
            case _: java.lang.Double =>
              bindConfigKey[Double](fullPath)
            case _ =>
              // Should be impossible.
              throw new IllegalArgumentException("config key produced bad number: " + value)
          }
        case ConfigValueType.STRING =>
          bindConfigKey[String](fullPath)
        case ConfigValueType.LIST =>
          // Figure out the list subtype. Note that there is no API call to handle this, so we try
          // methods in serial until one succeeds.
          val methods: Seq[() => Unit] = Seq(
            () => {
              fullConfig.apply[Seq[Config]](fullPath)
              bindConfigKey[Seq[Config]](fullPath)
            },
            () => {
              // Scala compiles a type in a constructor of Seq[Double] to Seq[Object], meaning we
              // need to bind as Seq[Object] in order for Guice to work.
              val value = fullConfig[Seq[Double]](fullPath).asInstanceOf[Seq[Object]]
              bind[Seq[Object]].annotatedWithName(fullPath).toInstance(value)
              bind[Option[Seq[Object]]].annotatedWithName(fullPath).toInstance(Some(value))
            },
            () => {
              val value = fullConfig.apply[Seq[Boolean]](fullPath).asInstanceOf[Seq[Object]]
              bind[Seq[Object]].annotatedWithName(fullPath).toInstance(value)
              bind[Option[Seq[Object]]].annotatedWithName(fullPath).toInstance(Some(value))
            },
            () => {
              // All values will parse as strings, which is odd, so this is last.
              fullConfig.apply[Seq[String]](fullPath)
              bindConfigKey[Seq[String]](fullPath)
            }
          )
          // Lazily apply the first method that works.
          val success = methods.iterator.map(method => Try(method())).exists(_.isSuccess)
          if (!success) {
            logger.warn(
              s"Could not find list type for key '$fullPath' in in " +
                s"${getClass.getSimpleName}. No value will be bound to '$fullPath'."
            )
          }
        case ConfigValueType.OBJECT =>
          bindConfigKey[Config](fullPath)
          // Recurse.
          bindConfigObject(
            config.toConfig()[Config](ConfigUtil.quoteString(key)).root,
            fullPathElements
          )
        case other =>
          // Shouldn't happen - but warn if it does.
          logger.warn(s"Unhandled config value type [$other] for key $fullPath")
      }
    }
  }

  /** Binds all of the paths in the full config object to the appropriate type, annotated with a
    * @@Named annotation holding the config key.
    */
  private def bindConfig(): Unit = {
    bindConfigObject(fullConfig.root, Seq.empty)
    // Adds default None bindings @Named Options.
    bind[Option[Boolean]].annotatedWith(classOf[Named]).toInstance(None)
    bind[Option[Int]].annotatedWith(classOf[Named]).toInstance(None)
    bind[Option[Long]].annotatedWith(classOf[Named]).toInstance(None)
    bind[Option[Double]].annotatedWith(classOf[Named]).toInstance(None)
    bind[Option[Config]].annotatedWith(classOf[Named]).toInstance(None)
    bind[Option[Seq[Config]]].annotatedWith(classOf[Named]).toInstance(None)
    bind[Option[Seq[String]]].annotatedWith(classOf[Named]).toInstance(None)
    bind[Option[Seq[Object]]].annotatedWith(classOf[Named]).toInstance(None)
    bind[Option[String]].annotatedWith(classOf[Named]).toInstance(None)
  }
}
