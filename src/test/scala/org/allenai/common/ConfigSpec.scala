package org.allenai.common

import com.typesafe.config.ConfigException
import org.allenai.common.testkit.UnitSpec
import org.allenai.common.Config._

import com.typesafe.config.ConfigFactory
import com.typesafe.config.{ Config => TypesafeConfig }
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class ConfigSpec extends UnitSpec {
  
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
    "null" -> null
  )

  val testConfig = ConfigFactory.parseMap(testConfigMap.asJava)

  "get[T]" should "work for String" in {
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

  "getScalaDuration(key, timeUnit)" should "work" in {
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

  "JSON serialization" should "work" in {
    val json = testConfig.toJson
    val deserialized = json.convertTo[TypesafeConfig]
    assert(deserialized === testConfig)
  }
}
