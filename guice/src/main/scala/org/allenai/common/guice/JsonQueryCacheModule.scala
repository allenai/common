package org.allenai.common.guice

import org.allenai.common.cache.JsonQueryCache

import com.google.inject.Provides
import com.google.inject.name.Named
import com.typesafe.config.{ Config, ConfigFactory }
import redis.clients.jedis.{ JedisPool, JedisPoolConfig }
import spray.json.JsonFormat

class JsonQueryCacheModule(config: Config = ConfigFactory.empty) extends ConfigModule(config) {
  override def configName: String = "cache.conf"

  override def bindingPrefix: Option[String] = Some("redis")

  @Provides def provideCache[V: JsonFormat](
    @Named("redis.hostname") hostname: String,
    @Named("redis.port") port: Int,
    @Named("redis.timeoutMillis") timeoutMillis: Int,
    @Named("redis.clientPrefix") clientPrefix: String
  ): JsonQueryCache[V] = new JsonQueryCache[V](
    new JedisPool(new JedisPoolConfig, hostname, port, timeoutMillis),
    clientPrefix
  )
}
