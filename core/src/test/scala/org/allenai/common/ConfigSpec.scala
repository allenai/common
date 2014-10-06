package org.allenai.common

import org.allenai.common.testkit.UnitSpec
import org.allenai.common.Config._

import com.typesafe.config.{ Config => TypesafeConfig, _ }
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConverters._
import scala.concurrent.duration._

import java.net.URI

// scalastyle:off magic.number
class ConfigSpec extends UnitSpec {

  def createConfig(map: Map[String, Any]): TypesafeConfig = {
    ConfigFactory.parseMap(map.asJava)
  }

  val testConfigMap: Map[String, Any] = Map(
    "string" -> "Hello world",
    "int" -> Int.MaxValue,
    "long" -> Long.MaxValue,
    "double" -> 1234.5678,
    "bool" -> true,
    "stringList" -> Seq("one", "two", "three").asJava,
    "intList" -> Seq(1, 2, 3, 4).asJava,
    "boolList" -> Seq(true, false, true).asJava,
    "doubleList" -> Seq(1.0, 2.2, 3.1415).asJava,
    "duration" -> "5 seconds",
    "uri" -> "http://www.example.com?q=hello&r=world",
    "null" -> null,
    "object" -> Map("foo" -> "bar").asJava,
    "objectList" -> Seq(Map("foo" -> "bar").asJava, Map("one" -> "two").asJava).asJava)

  val testConfig = createConfig(testConfigMap)

  "ConfigReader.map[A]" should "generate a new ConfigReader[A]" in {
    case class Stringy(value: String)
    implicit val stringyConfigReader = ConfigReader.stringReader map { value => Stringy(value) }
    val stringy = testConfig.get[Stringy]("string")
    assert(stringy === Some(Stringy("Hello world")))
  }

  "config.get[T]" should "work for String" in {
    assert(testConfig.get[String]("string") === Some("Hello world"))
  }

  it should "work for Int" in {
    assert(testConfig.get[Int]("int") === Some(Int.MaxValue))
  }

  it should "work for Long" in {
    assert(testConfig.get[Long]("long") === Some(Long.MaxValue))
  }

  it should "work for Double" in {
    assert(testConfig.get[Double]("double") === Some(1234.5678))
  }

  it should "work for Boolean" in {
    assert(testConfig.get[Boolean]("bool") === Some(true))
  }

  it should "work for ConfigValue" in {
    assert((testConfig.get[ConfigValue]("string") map { _.unwrapped }) === Some("Hello world"))
  }

  it should "work for Seq[String]" in {
    assert(testConfig.get[Seq[String]]("stringList") === Some(Seq("one", "two", "three")))
  }

  it should "work for Seq[Int]" in {
    assert(testConfig.get[Seq[Int]]("intList") === Some(Seq(1, 2, 3, 4)))
  }

  it should "work for Seq[Long]" in {
    assert(testConfig.get[Seq[Long]]("intList") === Some(Seq(1L, 2L, 3L, 4L)))
  }

  it should "work for Seq[Boolean]" in {
    assert(testConfig.get[Seq[Boolean]]("boolList") === Some(Seq(true, false, true)))
  }

  it should "work for Seq[Double]" in {
    assert(testConfig.get[Seq[Double]]("doubleList") === Some(Seq(1.0, 2.2, 3.1415)))
  }

  it should "work for Seq[ConfigValue]" in {
    assert((testConfig.get[Seq[ConfigValue]]("intList") map { _ map { _.unwrapped } }) ===
      Some(Seq(1, 2, 3, 4)))
  }

  it should "work for URI" in {
    assert(testConfig.get[URI]("uri") === Some(new URI("http://www.example.com?q=hello&r=world")))
  }

  it should "work for com.typesafe.config.Config" in {
    assert(testConfig.get[TypesafeConfig]("object") === Some(createConfig(Map("foo" -> "bar"))))
  }

  it should "work for Seq[com.typesafe.config.Config]" in {
    assert(testConfig.get[Seq[TypesafeConfig]]("objectList") === Some(
      Seq(createConfig(Map("foo" -> "bar")), createConfig(Map("one" -> "two")))))
  }

  // non-happy path cases

  it should "return None when key missing" in {
    assert(testConfig.get[String]("missing") === None)
  }

  it should "return None when value is null" in {
    assert(testConfig.get[String]("null") === None)
  }

  it should "raise ConfigException.WrongType" in {
    intercept[ConfigException.WrongType] {
      testConfig.get[Int]("string")
    }
  }

  "config.apply[T]" should "work for String" in {
    assert(testConfig[String]("string") === "Hello world")
  }

  it should "work for Int" in {
    assert(testConfig[Int]("int") === Int.MaxValue)
  }

  it should "work for Long" in {
    assert(testConfig[Long]("long") === Long.MaxValue)
  }

  it should "work for Double" in {
    assert(testConfig[Double]("double") === 1234.5678)
  }

  it should "work for Boolean" in {
    assert(testConfig[Boolean]("bool") === true)
  }

  it should "work for Seq[String]" in {
    assert(testConfig[Seq[String]]("stringList") === Seq("one", "two", "three"))
  }

  it should "work for Seq[Int]" in {
    assert(testConfig[Seq[Int]]("intList") === Seq(1, 2, 3, 4))
  }

  it should "work for Seq[Long]" in {
    assert(testConfig[Seq[Long]]("intList") === Seq(1L, 2L, 3L, 4L))
  }

  it should "work for Seq[Boolean]" in {
    assert(testConfig[Seq[Boolean]]("boolList") === Seq(true, false, true))
  }

  it should "work for Seq[Double]" in {
    assert(testConfig[Seq[Double]]("doubleList") === Seq(1.0, 2.2, 3.1415))
  }

  it should "work for URI" in {
    assert(testConfig[URI]("uri") === new URI("http://www.example.com?q=hello&r=world"))
  }

  "config.getScalaDuration(key, timeUnit)" should "work" in {
    assert(testConfig.getScalaDuration("duration", SECONDS) === Some(5.seconds))
  }

  it should "return None when key missing" in {
    assert(testConfig.getScalaDuration("missing", SECONDS) === None)
  }

  it should "raise ConfigException.BadValue" in {
    intercept[ConfigException.BadValue] {
      testConfig.getScalaDuration("string", SECONDS)
    }
  }

  "config.fromJson" should "parse into specified type" in {
    case class Foo(name: String, age: Int)
    implicit val fooFormat = jsonFormat2(Foo.apply)
    val config = createConfig(Map("foo" -> Map("name" -> "Howard", "age" -> 35).asJava))
    assert(config.fromJson[Foo]("foo") === Foo("Howard", 35))
  }

  "config.getFromJson" should "parse into specified type" in {
    case class Foo(name: String, age: Int)
    implicit val fooFormat = jsonFormat2(Foo.apply)
    val config = createConfig(Map("foo" -> Map("name" -> "Howard", "age" -> 35).asJava))
    assert(config.getFromJson[Foo]("foo") === Some(Foo("Howard", 35)))
  }

  "JSON serialization" should "work with default format" in {
    val json = testConfig.toJson
    val deserialized = json.convertTo[TypesafeConfig]
    assert(deserialized === testConfig)
  }

  it should "work with concise format" in {
    val json = testConfig.toJson(ConciseTypesafeConfigFormat)
    val deserialized = json.convertTo[TypesafeConfig](ConciseTypesafeConfigFormat)
    assert(deserialized === testConfig)
  }

  it should "work with pretty format" in {
    val json = testConfig.toJson(PrettyTypesafeConfigFormat)
    val deserialized = json.convertTo[TypesafeConfig](PrettyTypesafeConfigFormat)
    assert(deserialized === testConfig)
  }
}
