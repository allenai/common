package org.allenai.common.cache

import org.allenai.common.testkit.UnitSpec

import com.google.inject.util.Modules
import com.google.inject.{ ConfigurationException, CreationException, Guice }
import com.typesafe.config.ConfigFactory
import net.codingwell.scalaguice.typeLiteral
import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector
import redis.clients.jedis.JedisPool
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

class JsonQueryCacheModuleSpec extends UnitSpec {
  import Foo._

  val hostConf = ConfigFactory.parseString("hostname=localhost")

  "JsonQueryCacheModule" should "fail when given config without a hostname" in {
    val badModule = new JsonQueryCacheModule[Int](ConfigFactory.empty)
    intercept[CreationException] {
      Guice.createInjector(badModule)
    }
  }

  it should "create caches that work" in {
    // Set up modules with mock jedis pools.
    val mockJedisModule = new MockJedisPoolModule("localhost")

    val stringModule = Modules.`override`(
      new JsonQueryCacheModule[String](hostConf)
    ).`with`(mockJedisModule)
    println(Guice.createInjector(stringModule).instance[JedisPool])

    val intModule = Modules.`override`(
      new JsonQueryCacheModule[Int](hostConf)
    ).`with`(mockJedisModule)

    val seqStringModule = Modules.`override`(
      new JsonQueryCacheModule[Seq[String]](hostConf)
    ).`with`(mockJedisModule)

    val fooModule = Modules.`override`(
      new JsonQueryCacheModule[Foo](hostConf)
    ).`with`(mockJedisModule)

    val queryCaches = new QueryCaches(
      Guice.createInjector(stringModule).instance[JsonQueryCache[String]],
      Guice.createInjector(intModule).instance[JsonQueryCache[Int]],
      Guice.createInjector(seqStringModule).instance[JsonQueryCache[Seq[String]]],
      Guice.createInjector(fooModule).instance[JsonQueryCache[Foo]]
    )

    assert(queryCaches.getAll().forall(_.isEmpty))
    queryCaches.putAll()
    assert(queryCaches.allThereAndEq())
    queryCaches.delAll()
    assert(queryCaches.getAll().forall(_.isEmpty))
  }

  it should "maintain type parameters in its internal bindings" in {
    // Set up module with mock jedis pool.
    val mockJedisModule = new MockJedisPoolModule("localhost")
    val fooModule = Modules.`override`(
      new JsonQueryCacheModule[Foo](hostConf)
    ).`with`(mockJedisModule)

    val injector = Guice.createInjector(fooModule)

    // Can't get around type erasure in the check here :(
    injector.instance[JsonQueryCache[Foo]] shouldBe a[JsonQueryCache[_]]

    // If this doesn't throw an exception, it means the module is dropping the type parameter and
    // binding to all JsonQueryCache[_].
    intercept[ConfigurationException] {
      injector.instance[JsonQueryCache[Long]]
    }
  }
}
