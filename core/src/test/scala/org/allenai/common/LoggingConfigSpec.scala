package org.allenai.common

import java.nio.file.Files

import ch.qos.logback.classic.Level
import org.allenai.common.testkit.UnitSpec

import scala.io.Source

class LoggingConfigSpec extends UnitSpec with Logging {
  "loggerConfig" should "work" in {
    val path = Files.createTempFile("nio-temp", ".tmp")
    path.toFile().deleteOnExit()

    loggerConfig
      .Logger("org.allenai.common")
      .reset()
      .addAppender(
        loggerConfig.newPatternLayoutEncoder("%-5level: %message%n"),
        loggerConfig.newFileAppender(path.toString)
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

  "loggerConfig" should "support html encoder" in {
    val path = Files.createTempFile("nio-temp2", ".tmp")
    path.toFile().deleteOnExit()

    loggerConfig
      .Logger("org.allenai.common")
      .reset()
      .addAppender(
        loggerConfig.newHtmlLayoutEncoder("%relative%thread%level%logger%msg"),
        loggerConfig.newFileAppender(path.toString)
      )
      .setLevel(Level.INFO)

    // Tags will be escaped for rendering inside table cell of HTML logger
    logger.info("<i>html</i>")

    assert(
      Source
        .fromFile(path.toString)
        .mkString
        .contains("<td class=\"Message\">&lt;i&gt;html&lt;/i&gt;</td>")
    )
  }
}
