package org.allenai.common

import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.html.HTMLLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core._
import ch.qos.logback.core.encoder.{Encoder, LayoutWrappingEncoder}
import org.slf4j.LoggerFactory

/** This trait is meant to be mixed into a class to provide logging and logging configuration.
  *
  * The enclosed methods provide a Scala-style logging signature where the
  * message is a block instead of a string.  This way the message string is
  * not constructed unless the message will be logged.
  */
trait Logging {
  val internalLogger = LoggerFactory.getLogger(this.getClass)

  object logger { // scalastyle:ignore
    def trace(message: => String): Unit =
      if (internalLogger.isTraceEnabled) {
        internalLogger.trace(message)
      }

    def debug(message: => String): Unit =
      if (internalLogger.isDebugEnabled) {
        internalLogger.debug(message)
      }

    def info(message: => String): Unit =
      if (internalLogger.isInfoEnabled) {
        internalLogger.info(message)
      }

    def warn(message: => String): Unit =
      if (internalLogger.isWarnEnabled) {
        internalLogger.warn(message)
      }

    def warn(message: => String, throwable: Throwable): Unit =
      if (internalLogger.isWarnEnabled) {
        internalLogger.warn(message, throwable)
      }

    def error(message: => String): Unit =
      if (internalLogger.isErrorEnabled) {
        internalLogger.error(message)
      }

    def error(message: => String, throwable: Throwable): Unit =
      if (internalLogger.isErrorEnabled) {
        internalLogger.error(message, throwable)
      }

    /** Simple logback configuration.
      * Hopefully this will be discoverable by just typing <code>logger.config().[TAB]</code>
      *
      * Examples:
      * <code>
      * logger.config("org.apache.spark").setLevel(Level.WARN)
      *
      * logger.config().addAppender(
      *   logger.factory.patternLayoutEncoder("%-5level [%thread]: %message%n"),
      *   logger.factory.consoleAppender
      * )
      * </code>
      *
      * @param loggerName the logger name, by default ROOT.
      */
    def config(loggerName: String = org.slf4j.Logger.ROOT_LOGGER_NAME) = {
      new Config(loggerName)
    }

    class Config(loggerName: String = org.slf4j.Logger.ROOT_LOGGER_NAME) {
      private val logger: Logger = LoggerFactory.getLogger(loggerName).asInstanceOf[Logger]

      /** Resets the logger. */
      def reset() = {
        logger.getLoggerContext.reset()
      }

      /** Simple log level setting. Example:
        * <code>
        * logger.config("org.apache.spark").setLevel(Level.WARN)
        * </code>
        */
      def setLevel(level: Level) = {
        logger.setLevel(level)
      }

      /** Simple log appender creation. Example:
        * <code>
        * logger.config().addAppender(
        *   logger.factory.patternLayoutEncoder("%-5level [%thread]: %message%n"),
        *   logger.factory.consoleAppender
        * )
        * logger.config().addAppender(
        *   logger.factory.htmlLayoutEncoder("%relative%thread%level%logger%msg"),
        *   logger.factory.fileAppender("./log.html")
        * )
        * </code>
        */
      def addAppender(
          encoder: Encoder[ILoggingEvent],
          appender: OutputStreamAppender[ILoggingEvent]
          ) = {
        val loggerContext = logger.getLoggerContext
        encoder.setContext(loggerContext)
        encoder.start()
        appender.setContext(loggerContext)
        appender.setEncoder(encoder)
        appender.start()
        logger.addAppender(appender)
      }
    }

    /** Factory methods for some simple config objects. */
    object factory {
      def patternLayoutEncoder(pattern: String) = {
        val encoder = new PatternLayoutEncoder()
        encoder.setPattern(pattern)
        encoder
      }

      def htmlLayoutEncoder(pattern: String) = {
        new LayoutWrappingEncoder[ILoggingEvent] {
          private val htmlLayout = new HTMLLayout()
          htmlLayout.setPattern(pattern)
          setLayout(layout)

          override def setLayout(layout: Layout[ILoggingEvent]) = {
            throw new Exception("Layout set via Logging.logger.config.htmlLayoutEncoder")
          }

          override def setContext(loggerContext: Context) = {
            htmlLayout.setContext(loggerContext)
            super.setContext(loggerContext)
          }

          override def start() = {
            htmlLayout.start()
            super.start()
          }
        }
      }

      def consoleAppender() = new ConsoleAppender[ILoggingEvent]()

      def fileAppender(fileName: String) = {
        val appender = new FileAppender[ILoggingEvent]()
        appender.setAppend(false)
        appender.setFile(fileName)
        appender
      }
    }
  }
}
