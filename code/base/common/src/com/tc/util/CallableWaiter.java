/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CallableWaiter {
  private static final int DEFAULT_CHECK_INTERVAL   = 1 * 1000;
  private static final int DEFAULT_CALLABLE_TIMEOUT = 5 * 60 * 1000;

  public static void waitOnCallable(final Callable<Boolean> callable) throws Exception {
    waitOnCallable(callable, DEFAULT_CALLABLE_TIMEOUT);
  }

  public static void waitOnCallable(final Callable<Boolean> callable, final long timeoutMs) throws Exception {
    waitOnCallable(callable, timeoutMs, DEFAULT_CHECK_INTERVAL);
  }

  public static void waitOnCallable(final Callable<Boolean> callable, final long timeoutMs, final int interval)
      throws Exception {
    final long start = System.nanoTime();
    try {
      while (!callable.call()) {
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= timeoutMs) { throw new TCTimeoutException(
                                                                                                                  "Timed out waiting for callable after "
                                                                                                                      + timeoutMs
                                                                                                                      + "ms"); }
        ThreadUtil.reallySleep(interval);
      }
      return;
    } catch (Exception e) {
      throw e;
    }
  }
}
