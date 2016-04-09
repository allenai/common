package org.allenai.common.cache

import com.fiftyonred.mock_jedis.MockJedisPool
import net.codingwell.scalaguice.ScalaModule
import redis.clients.jedis.{ JedisPool, JedisPoolConfig }

/** Test module that provides mock Jedis pools instead of real ones. */
class MockJedisPoolModule(hostname: String) extends ScalaModule {

  override def configure(): Unit = {
    bind[JedisPool].toInstance(new MockJedisPool(new JedisPoolConfig, hostname))
  }
}
