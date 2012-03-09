/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.test.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class WaitUtil {
  private static final long MAX_WAIT_SECONDS = 300;

  public static void waitUntilCallableReturnsTrue(Callable<Boolean> callable) throws Exception {
    waitUntil(callable, true, 1000);
  }

  public static void waitUntilCallableReturnsFalse(Callable<Boolean> callable) throws Exception {
    waitUntil(callable, false, 1000);
  }

  public static void waitUntil(Callable<Boolean> callable, boolean until, long delayInMillis) throws Exception {
    long start = System.nanoTime();
    while (true) {
      boolean rv = callable.call();
      debug("Waiting until callable returns: " + until + ", returned: " + rv);
      if (rv == until) {
        break;
      }
      Thread.sleep(delayInMillis);
      if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) > MAX_WAIT_SECONDS) { throw new AssertionError(
                                                                                                                   "Max wait time over! Callable never returned: "
                                                                                                                       + until); }
    }
  }

  private static void debug(String str) {
    System.out.println("[WAIT UTIL] '" + Thread.currentThread().getName() + "' : " + str);
  }
}
