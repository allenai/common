package org.allenai.common

import java.nio.file.Files

import ch.qos.logback.classic.Level
import org.allenai.common.testkit.UnitSpec

import scala.io.Source

class LoggingConfigSpec extends UnitSpec with Logging {
  "logging.config" should "work" in {
    val path = Files.createTempFile("nio-temp", ".tmp")
    path.toFile().deleteOnExit()

    val l = logger.config("org.allenai.common")
    l.reset()
    l.addAppender(
      logger.factory.patternLayoutEncoder("%-5level [%thread]: %message%n"),
      logger.factory.fileAppender(path.toString)
    )
    l.setLevel(Level.WARN)
    logger.info("info should not be visible")
    logger.warn("warn should be visible")
    logger.warn("warn should be visible 2")

    assert(
      Source.fromFile(path.toString).mkString ===
      """WARN  [ScalaTest-run-running-LoggingConfigSpec]: warn should be visible
        |WARN  [ScalaTest-run-running-LoggingConfigSpec]: warn should be visible 2
        |""".stripMargin)

    println(Source.fromFile(path.toString).mkString)
  }
}
