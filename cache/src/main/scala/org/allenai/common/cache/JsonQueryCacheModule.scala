package org.allenai.common.cache

import org.allenai.common.guice.ConfigModule

import com.google.inject.Provides
import com.google.inject.name.Named
import com.typesafe.config.Config
import redis.clients.jedis.{ JedisPool, JedisPoolConfig }
import spray.json.JsonFormat

/** A guice module for binding a `JsonQueryCache` instance with a parameterized value type. */
class JsonQueryCacheModule[V](config: Config)(
  implicit
  jsonFormat: JsonFormat[V],
  manifest: Manifest[V]
) extends ConfigModule(config) {

  override def configName: String = "cache.conf"

  override def bindingPrefix: Option[String] = Some("redis")

  override def configureWithConfig(fullConfig: Config): Unit = {
    bind[JsonFormat[V]].toInstance(jsonFormat)
  }

  @Provides def provideJedisPool(
    @Named("redis.hostname") hostname: String,
    @Named("redis.port") port: Int,
    @Named("redis.timeoutMillis") timeoutMillis: Int
  ): JedisPool = new JedisPool(new JedisPoolConfig, hostname, port, timeoutMillis)
}
