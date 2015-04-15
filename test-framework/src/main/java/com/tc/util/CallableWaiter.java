/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util;


import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


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
    while (!callable.call()) {
      if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= timeoutMs) { throw new TimeoutException(
                                                                                                                "Timed out waiting for callable after "
                                                                                                                    + timeoutMs
                                                                                                                    + "ms"); }
      ThreadUtil.reallySleep(interval);
    }
    return;
  }
}
