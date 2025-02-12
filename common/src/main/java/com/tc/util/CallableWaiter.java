/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util;

import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CallableWaiter {
  private static final int DEFAULT_CHECK_INTERVAL   = 1 * 1000;
  private static final int DEFAULT_CALLABLE_TIMEOUT = 5 * 60 * 1000;

  public static void waitOnCallable(Callable<Boolean> callable) throws Exception {
    waitOnCallable(callable, DEFAULT_CALLABLE_TIMEOUT);
  }

  public static void waitOnCallable(Callable<Boolean> callable, long timeoutMs) throws Exception {
    waitOnCallable(callable, timeoutMs, DEFAULT_CHECK_INTERVAL);
  }

  public static void waitOnCallable(Callable<Boolean> callable, long timeoutMs, int interval)
      throws Exception {
    final long start = System.nanoTime();
    while (!callable.call()) {
      if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= timeoutMs) { throw new TCTimeoutException(
                                                                                                                "Timed out waiting for callable after "
                                                                                                                    + timeoutMs
                                                                                                                    + "ms"); }
      ThreadUtil.reallySleep(interval);
    }
    return;
  }
}
