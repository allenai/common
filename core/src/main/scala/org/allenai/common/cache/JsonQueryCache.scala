package org.allenai.common.cache

import org.allenai.common.Logging

import redis.clients.jedis.{ Jedis, JedisPool }

import spray.json._

/** class holding a Redis cache of query results. This is meant to store any value T where
  * T : JsonFormat (any T with a json serialization as per spray json)
  * , keyed on string query. If we ever need to run multiple instances within the same application
  * user neeeds to handle using a shared Jedis Pool.
  * @param pool the JedisPool that the client should use to serve requests
  * @param clientPrefix an identifier for the client using this caching mechanism, which will become
  * part of the cache key (prepended to the actual query)
  */
class JsonQueryCache[V: JsonFormat](pool: JedisPool, clientPrefix: String)
    extends Logging {

  /**
  * constructs a QueryCache[V], handling the Jedis pool itself
  * @param redisHostName the hostName of the redis server to connect to
  * @param redisHostPort the port of the redis server to connect to
  * @param clientPrefix an identifier for the client using this caching mechanism, which will become
  * part of the cache key (prepended to the actual query)
  */
  def this[V](redisHostName: String, redisHostPort: Int, clientPrefix: String) =
    this[V](new JedisPool(redisHostName, redisHostPort), clientPrefix)

  /** Trivial Helper to construct cache key with client prefix.
    */
  protected def keyForQuery(query: String): String = s"${clientPrefix}_$query"

  /** Retrieves the value for a passed key
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

  /** Puts a key->value to the cache
    * @param query key for value (not including client prefix)
    * @param response Value you want stored in cache
    */
  def put(query: String, response: V): Unit = withResource[Unit] { client: Jedis =>
    client.set(keyForQuery(query), response.toJson.compactPrint)
  }

  /** deletes a key->value from the cache
    * @param query key for value you want to delete (not including client prefix)
    */
  def del(query: String): Unit = withResource[Unit] { client: Jedis =>
    client.del(keyForQuery(query))
  }

  // wrap operation in a try-catch & handle closing resource
  private def withResource[T](operation: (Jedis => T)) = {
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

