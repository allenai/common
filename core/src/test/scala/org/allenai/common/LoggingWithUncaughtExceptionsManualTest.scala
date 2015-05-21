package org.allenai.common

object LoggingWithUncaughtExceptionsManualTest extends LoggingWithUncaughtExceptions {
  def main(args: Array[String]): Unit = {
    throw new Exception("uncaught")
    // Operator, please eyeball the output for something like:
    // 11:40:30.741 [main] ERROR o.allenai.common.LoggingManualTest$ - Uncaught exception in thread: main
    // java.lang.Exception: uncaught
    // at org.allenai.common.LoggingManualTest$.main(LoggingManualTest.scala:5) ~[test-classes/:na]
    // at org.allenai.common.LoggingManualTest.main(LoggingManualTest.scala) ~[test-classes/:na]
    // at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_25]
    // at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_25]
    // at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_25]
    // at java.lang.reflect.Method.invoke(Method.java:483) ~[na:1.8.0_25]
    // at com.intellij.rt.execution.application.AppMain.main(AppMain.java:134) ~[idea_rt.jar:na]
  }
}
