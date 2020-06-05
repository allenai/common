package org.allenai.common

// Intended for usage in main objects / Apps so we don't lose track of uncaught exceptions.
// Alternative: use a selftype of Logging, as jessek@allenai.org suggested.
trait LoggingWithUncaughtExceptions extends Logging {
  Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
    def uncaughtException(t: Thread, e: Throwable): Unit = {
      logger.error("Uncaught exception in thread: " + t.getName, e)
    }
  })
}
