package com.tc.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeoutTimerConfig {
  public static final long                DEFAULT_TIMEOUT_THRESHOLD_IN_MILLIS = 60000;
  public static final boolean             DEFAULT_DUMP_THREADS_ON_TIMEOUT     = true;
  public static final int                 DEFAULT_NUM_THREAD_DUMPS            = 3;
  public static final long                DEFAULT_DUMP_INTERVAL_IN_MILLIS     = 500;

  long timeoutThresholdInMillis() default DEFAULT_TIMEOUT_THRESHOLD_IN_MILLIS;

  // controls for thread dumping.
  boolean dumpThreadsOnTimeout() default DEFAULT_DUMP_THREADS_ON_TIMEOUT;
  int numThreadDumps() default DEFAULT_NUM_THREAD_DUMPS;
  long dumpIntervalInMillis() default DEFAULT_DUMP_INTERVAL_IN_MILLIS;

  // extension lambdas
  String beforeTimeout() default "";
  String doDumpServerDetails() default "";
}
