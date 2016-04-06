package org.allenai.common.cache

import org.allenai.common.testkit.UnitSpec

import com.fiftyonred.mock_jedis.MockJedisPool
import redis.clients.jedis.JedisPoolConfig
import spray.json.DefaultJsonProtocol._

case class Foo(stringVar: String, intVar: Int)

class QueryCaches {
  implicit val fooFormat = jsonFormat2(Foo)

  val mockJedisPool = new MockJedisPool(new JedisPoolConfig, "localhost")

  val stringQueryCache = new JsonQueryCache[String](mockJedisPool, "test_string")
  val intQueryCache = new JsonQueryCache[Int](mockJedisPool, "test_int")
  val seqStringQueryCache = new JsonQueryCache[Seq[String]](mockJedisPool, "test_seq")

  // It's an object I can test
  val fooQueryCache = new JsonQueryCache[Foo](mockJedisPool, "test_foo")

  val stringKey = "stringKey"
  val stringValue = "stringValue"

  val intKey = "intKey"
  val intValue = 32

  val seqStringKey = "seqStringKey"
  val seqStringValue = Seq("a string", "a second string", "third time's the charm")

  val fooKey = "fooKey"
  val fooValue = new Foo("stringerino", 42)

  def getAll(): Seq[Option[Any]] = Seq(
    stringQueryCache.get(stringKey),
    intQueryCache.get(intKey),
    seqStringQueryCache.get(seqStringKey),
    fooQueryCache.get(fooKey)
  )

  def putAll(): Unit = {
    stringQueryCache.put(stringKey, stringValue)
    intQueryCache.put(intKey, intValue)
    seqStringQueryCache.put(seqStringKey, seqStringValue)
    fooQueryCache.put(fooKey, fooValue)
  }

  def delAll(): Unit = {
    stringQueryCache.del(stringKey)
    intQueryCache.del(intKey)
    seqStringQueryCache.del(seqStringKey)
    fooQueryCache.del(fooKey)
  }

  def allThereAndEq(): Boolean = {
    stringQueryCache.get(stringKey).exists(_.equals(stringValue)) &&
      intQueryCache.get(intKey).exists(_.equals(intValue)) &&
      seqStringQueryCache.get(seqStringKey).exists(_.equals(seqStringValue)) &&
      fooQueryCache.get(fooKey).exists(_.equals(fooValue))
  }
}

class JsonQueryCacheSpec extends UnitSpec {

  val queryCaches = new QueryCaches

  "JsonQueryCache" should "return None when items are not in cache" in {
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
}
