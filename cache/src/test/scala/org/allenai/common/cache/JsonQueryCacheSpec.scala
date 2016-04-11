package org.allenai.common.cache

import org.allenai.common.testkit.UnitSpec

import com.fiftyonred.mock_jedis.MockJedisPool
import redis.clients.jedis.JedisPoolConfig
import spray.json.DefaultJsonProtocol._

class JsonQueryCacheSpec extends UnitSpec {
  import Foo._

  val mockJedisPool = new MockJedisPool(new JedisPoolConfig, "localhost")
  val stringQueryCache = new JsonQueryCache[String]("test_string", mockJedisPool)
  val intQueryCache = new JsonQueryCache[Int]("test_int", mockJedisPool)
  val seqStringQueryCache = new JsonQueryCache[Seq[String]]("test_seq", mockJedisPool)
  val fooQueryCache = new JsonQueryCache[Foo]("test_foo", mockJedisPool)

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
