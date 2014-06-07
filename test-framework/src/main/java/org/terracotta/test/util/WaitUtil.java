/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.test.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

public class WaitUtil {
  private static final long MAX_WAIT_SECONDS = TimeUnit.MINUTES.toSeconds(3);

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

  public static <T> void assertThatWithin(Callable<T> check, Matcher<? super T> matcher, long time, TimeUnit timeUnit) throws Exception {
    long start = System.currentTimeMillis();
    long waited;
    while ((waited = System.currentTimeMillis() - start) < timeUnit.toMillis(time)) {
      if (matcher.matches(check.call())) {
        return;
      } else {
        Description description = new StringDescription();
        description.appendText("Waited " + waited + "ms for ");
        matcher.describeMismatch(check.call(), description);
        debug(description.toString());
      }
      ThreadUtil.reallySleep(1000);
    }
    assertThat(check.call(), matcher);
  }
}
