package org.allenai.common.cache

import org.allenai.common.Logging

import redis.clients.jedis.{ Jedis, JedisPool, JedisPoolConfig, Protocol }
import spray.json._

/** Class holding a Redis cache of query results. This is meant to store any value `T` where
  * `T : spray.json.JsonFormat` (any `T` with a json serialization as per spray json), keyed on
  * string query. Multiple cache instances (instances pointing to different Redis caches) need to be
  * configured to have different JedisPools.
  * @param pool the JedisPool that the client should use to serve requests
  * @param clientPrefix an identifier for the client using this caching mechanism, which will become
  * part of the cache key (prepended to the actual query)
  */
class JsonQueryCache[V: JsonFormat](pool: JedisPool, clientPrefix: String) extends Logging {
  /** Constructs a `QueryCache[V]`, building a JedisPool from the parameters given.
    * @param redisHostName the hostName of the redis server to connect to
    * @param redisHostPort the port of the redis server to connect to
    * @param redisTimeout the timeout, in millis, to use when sending requests to the redis server
    * @param clientPrefix an identifier for the client using this caching mechanism, which will
    * become part of the cache key (prepended to the actual query)
    */
  def this(redisHostName: String, redisHostPort: Int, redisTimeout: Int, clientPrefix: String) =
    this(new JedisPool(new JedisPoolConfig, redisHostName, redisHostPort, redisTimeout), clientPrefix)

  /** Constructs a `QueryCache[V]`, building a JedisPool from the parameters given.
    * Uses the default Jedis timeout for requests.
    * @param redisHostName the hostName of the redis server to connect to
    * @param redisHostPort the port of the redis server to connect to
    * @param clientPrefix an identifier for the client using this caching mechanism, which will
    * become part of the cache key (prepended to the actual query)
    */
  def this(redisHostName: String, redisHostPort: Int, clientPrefix: String) =
    this(redisHostName, redisHostPort, Protocol.DEFAULT_TIMEOUT, clientPrefix)

  /** @return the cache key for the query, with client prefix prepended */
  protected def keyForQuery(query: String): String = s"${clientPrefix}_$query"

  /** Retrieves the value for a passed key.
    * @param query key for stored value (not including client prefix)
    * @return Option containing value, None if not found or timed out (async)
    */
  def get(query: String): Option[V] = {
    withResource[Option[V]] { client: Jedis =>
      Option(client.get(keyForQuery(query))) map { response: String =>
        response.parseJson.convertTo[V]
      }
    }
  }

  /** Puts a key->value pair in the cache.
    * @param query key for value (not including client prefix)
    * @param response Value you want stored in cache
    */
  def put(query: String, response: V): Unit = withResource[Unit] { client: Jedis =>
    client.set(keyForQuery(query), response.toJson.compactPrint)
  }

  /** Deletes a key->value pair from the cache.
    * @param query key for value you want to delete (not including client prefix)
    */
  def del(query: String): Unit = withResource[Unit] { client: Jedis =>
    client.del(keyForQuery(query))
  }

  /** Runs the given operation, handling fetching and closing the Jedis connection. */
  private def withResource[T](operation: (Jedis => T)): T = {
    var resource: Jedis = null
    try {
      resource = pool.getResource
      operation(resource)
    } finally {
      if (resource != null) {
        resource.close()
      }
    }
  }
}

