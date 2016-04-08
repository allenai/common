package org.allenai.common.cache

import org.allenai.common.guice.ConfigModule

import com.google.inject.Provides
import com.google.inject.name.Named
import com.typesafe.config.{ Config, ConfigFactory }
import redis.clients.jedis.{ JedisPool, JedisPoolConfig }

class JsonQueryCacheModule(config: Config = ConfigFactory.empty) extends ConfigModule(config) {
  override def configName: String = "cache.conf"

  override def bindingPrefix: Option[String] = Some("redis")

  @Provides def provideJedisPool(
    @Named("redis.hostname") hostname: String,
    @Named("redis.port") port: Int,
    @Named("redis.timeoutMillis") timeoutMillis: Int
  ): JedisPool = new JedisPool(new JedisPoolConfig, hostname, port, timeoutMillis)
}
