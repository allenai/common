package org.allenai.common

import org.slf4j.LoggerFactory

/** This trait is meant to be mixed into a class to provide logging.
  *
  * The enclosed methods provide a Scala-style logging signature where the
  * message is a block instead of a string.  This way the message string is
  * not constructed unless the message will be logged.
  */
trait Logging {
  val internalLogger = LoggerFactory.getLogger(this.getClass)

  object logger {
    def trace(message: =>String) =
      if (internalLogger.isTraceEnabled) {
        internalLogger.trace(message)
      }

    def debug(message: =>String) =
      if (internalLogger.isDebugEnabled) {
        internalLogger.debug(message)
      }

    def info(message: =>String) =
      if (internalLogger.isInfoEnabled) {
        internalLogger.info(message)
      }

    def warn(message: =>String) =
      if (internalLogger.isWarnEnabled) {
        internalLogger.warn(message)
      }

    def warn(message: =>String, throwable: Throwable) =
      if (internalLogger.isWarnEnabled) {
        internalLogger.warn(message, throwable)
      }

    def error(message: =>String) =
      if (internalLogger.isErrorEnabled) {
        internalLogger.error(message)
      }

    def error(message: =>String, throwable: Throwable) =
      if (internalLogger.isErrorEnabled) {
        internalLogger.error(message, throwable)
      }
  }
}
