package org.allenai.wumpus.client

import org.allenai.common.Logging

import java.nio.file.{ Files, Paths }

import redis.clients.jedis._

/** Class holding a Redis cache of query results. This is meant to store mostly-raw responses,
  * keyed on raw query. "Mostly-raw" means split into lines, with the status line removed -
  * what the WumpusClient returns. Note that this will grow forever, which may not be great.
  * Also, a Redis pool is created per Wumpus Client currently. If we ever need to run multiple
  * instances of Wumpus Client within the same solver/application, this code needs to be changed
  * to inject a shared Jedis Pool via the WumpusModule instead of creating a new one.
  * @param redisHostname name of host where redis server is running
  * @param clientPrefix an identifier for the client using this caching mechanism, which will become
  * part of the cache key (prepended to the actual query)
  */
class QueryCache(redisHostname: String, clientPrefix: String) extends Logging {

  val jedisPool = new JedisPool(new JedisPoolConfig, redisHostname)

  private val Newline = """\n""".r

  /** Trivial Helper to construct cache key with client prefix.
    */
  private def keyForQuery(query: String): String = s"${clientPrefix}_$query"

  /** @return the cached result for the given query, or None if it isn't in the cache */
  def get(query: String): Option[Seq[String]] = {
    val cacheKey = keyForQuery(query)
    withJedis { jedis =>
      val cachedResult = jedis.get(cacheKey)
      if (cachedResult != null) {
        logger.trace(s"Found in cache: key: ${cacheKey}, value: ${cachedResult}")
        Some(Newline.split(cachedResult))
      } else {
        None
      }
    }
  }

  /** Stores the given query / response pair in the cache. */
  def put(query: String, response: Seq[String]): Unit = {
    val cacheKey = keyForQuery(query)
    withJedis { jedis =>
      jedis.set(cacheKey, response.mkString("\n"))
      logger.trace(s"Added query ${cacheKey} to cache")
    }
  }

  /** Deletes any cached result for the given query. */
  def delete(query: String): Unit = {
    val cacheKey = keyForQuery(query)
    withJedis { jedis =>
      jedis.del(cacheKey)
      logger.trace(s"Deleted query ${cacheKey} from cache")
    }
  }

  /** Helper Method to wrap Jedis operations in a try-finally
    */
  private def withJedis[T](operation: (Jedis => T)): T = {
    var jedis: Jedis = null
    try {
      jedis = jedisPool.getResource
      operation(jedis)
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
  }
}

