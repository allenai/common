package org.allenai.common.cache

import org.allenai.common.{ GitVersion, Version, cache }
import org.allenai.common.testkit.UnitSpec
import org.scalatest.BeforeAndAfterAll
import sys.process._
import spray.json.DefaultJsonProtocol._

class QueryCaches(redisHostname: String, redisPort: Int) {
  val stringQueryCache = new JsonQueryCache[String](redisHostname, redisPort, "test")
  val intQueryCache = new JsonQueryCache[Int](redisHostname, redisPort, "test")
  val seqStringQueryCache = new JsonQueryCache[Seq[String]](redisHostname, redisPort, "test")
  // It's an object I can test
  val versionQueryCache = new JsonQueryCache[Version](redisHostname, redisPort, "test")

  val stringKey = "stringKey"
  val stringValue = "stringValue"

  val intKey = "intKey"
  val intValue = 32

  val seqStringKey = "seqStringKey"
  val seqStringValue = Seq("a string", "a second string", "third time's the charm")

  val versionKey = "versionKey"
  val versionValue = new Version(new GitVersion("test", 324, Option("tester")), "artifact")

  def getAll(): Seq[Option[Any]] = Seq(
    stringQueryCache.get(stringKey),
    intQueryCache.get(intKey),
    seqStringQueryCache.get(seqStringKey),
    versionQueryCache.get(versionKey)
  )

  def putAll(): Unit = {
    stringQueryCache.put(stringKey, stringValue)
    intQueryCache.put(intKey, intValue)
    seqStringQueryCache.put(seqStringKey, seqStringValue)
    versionQueryCache.put(versionKey, versionValue)
  }

  def delAll(): Unit = {
    stringQueryCache.del(stringKey)
    intQueryCache.del(intKey)
    seqStringQueryCache.del(seqStringKey)
    versionQueryCache.del(versionKey)
  }

  def allThereAndEq(): Boolean = {
    stringQueryCache.get(stringKey).exists(_.equals(stringValue))
    intQueryCache.get(intKey).exists(_.equals(intValue))
    seqStringQueryCache.get(seqStringKey).exists(_.equals(seqStringValue))
    versionQueryCache.get(versionKey).exists(_.equals(versionValue))
  }
}

class QueryCacheSpec extends UnitSpec with BeforeAndAfterAll {

  val redisHostname = "127.0.0.1"
  val redisPort = 6379
  val redisServer = "redis-server".run()
  val queryCaches = new QueryCaches(redisHostname, redisPort)

  "QueryCache" should "return None when items are not in cache" in {
    assert(queryCaches.getAll().forall(_.isEmpty))
  }

  it should "put the items in properly and let us get them back" in {
    queryCaches.putAll()
    assert(queryCaches.allThereAndEq())
  }

  it should "delete the items properly" in {
    queryCaches.delAll()
    assert(queryCaches.getAll().forall(_.isEmpty))
  }

  override def afterAll() {
    Seq("redis-cli", "FLUSHALL").!!
    Seq("redis-cli", "SHUTDOWN").!!
  }
}
