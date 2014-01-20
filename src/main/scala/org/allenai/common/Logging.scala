package org.allenai.common

import org.slf4j.LoggerFactory

/** This trait is meant to be mixed into a class to provide logging.
  *
  * The enclosed methods provide a Scala-style logging signature where the
  * message is a block instead of a string.  This way the message string is
  * not constructed unless the message will be logged.
  */
trait Logging {
  val logger = LoggerFactory.getLogger(this.getClass)

  object log {
    def trace(message: =>String) =
      if (logger.isTraceEnabled) {
        logger.trace(message)
      }

    def debug(message: =>String) =
      if (logger.isDebugEnabled) {
        logger.debug(message)
      }

    def info(message: =>String) =
      if (logger.isInfoEnabled) {
        logger.info(message)
      }

    def warn(message: =>String) =
      if (logger.isWarnEnabled) {
        logger.warn(message)
      }

    def warn(message: =>String, throwable: Throwable) =
      if (logger.isWarnEnabled) {
        logger.warn(message, throwable)
      }

    def error(message: =>String) =
      if (logger.isErrorEnabled) {
        logger.error(message)
      }

    def error(message: =>String, throwable: Throwable) =
      if (logger.isErrorEnabled) {
        logger.error(message, throwable)
      }
  }
}
