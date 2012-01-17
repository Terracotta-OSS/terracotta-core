/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.test.util;

import java.util.concurrent.Callable;

public class WaitUtil {

  public static void waitUntilCallableReturnsTrue(Callable<Boolean> callable) throws Exception {
    waitUntil(callable, true, 1000);
  }

  public static void waitUntilCallableReturnsFalse(Callable<Boolean> callable) throws Exception {
    waitUntil(callable, false, 1000);
  }

  public static void waitUntil(Callable<Boolean> callable, boolean until, long delayInMillis) throws Exception {
    while (true) {
      boolean rv = callable.call();
      debug("Waiting until callable returns: " + until + ", returned: " + rv);
      if (rv == until) {
        break;
      }
      Thread.sleep(delayInMillis);
    }
  }

  private static void debug(String str) {
    System.out.println("[WAIT UTIL] '" + Thread.currentThread().getName() + "' : " + str);
  }
}
