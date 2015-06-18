package org.allenai.common

import java.nio.file.Files

import ch.qos.logback.classic.Level
import org.allenai.common.testkit.UnitSpec

import scala.io.Source

class LoggingConfigSpec extends UnitSpec with Logging {
  "logging.config" should "work" in {
    val path = Files.createTempFile("nio-temp", ".tmp")
    path.toFile().deleteOnExit()

    logger.Config("org.allenai.common")
      .reset()
      .addAppender(
        logger.Config.newPatternLayoutEncoder("%-5level: %message%n"),
        logger.Config.newFileAppender(path.toString)
      )
      .setLevel(Level.WARN)

    logger.info("info should not be visible")
    logger.warn("warn should be visible")
    logger.warn("warn should be visible 2")

    assert(
      Source.fromFile(path.toString).mkString ===
        """WARN : warn should be visible
          |WARN : warn should be visible 2
          |""".stripMargin
    )
  }
}
