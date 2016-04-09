package org.allenai.common.cache

import org.allenai.common.testkit.UnitSpec

import com.fiftyonred.mock_jedis.MockJedisPool
import redis.clients.jedis.JedisPoolConfig
import spray.json.DefaultJsonProtocol._

class JsonQueryCacheSpec extends UnitSpec {
  import Foo._

  val mockJedisPool = new MockJedisPool(new JedisPoolConfig, "localhost")
  val stringQueryCache = new JsonQueryCache[String](mockJedisPool, "test_string")
  val intQueryCache = new JsonQueryCache[Int](mockJedisPool, "test_int")
  val seqStringQueryCache = new JsonQueryCache[Seq[String]](mockJedisPool, "test_seq")
  val fooQueryCache = new JsonQueryCache[Foo](mockJedisPool, "test_foo")

  val queryCaches = new QueryCaches(
    stringQueryCache,
    intQueryCache,
    seqStringQueryCache,
    fooQueryCache
  )

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
