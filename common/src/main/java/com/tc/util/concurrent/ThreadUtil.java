/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * Some shortcut stuff for doing common thread stuff
 *
 * @author steve
 */
public class ThreadUtil {

  public static void reallySleep(long millis) {
    reallySleep(millis, 0);
  }

  public static void reallySleep(TimeUnit unit, long sleepTime) {
    reallySleep(unit.toMillis(sleepTime));
  }

  public static void reallySleep(long millis, int nanos) {
    boolean interrupted = false;
    try {
      long millisLeft = millis;
      while (millisLeft > 0 || nanos > 0) {
        long start = System.currentTimeMillis();
        try {
          Thread.sleep(millisLeft, nanos);
        } catch (InterruptedException e) {
          interrupted = true;
        }
        millisLeft -= System.currentTimeMillis() - start;
        nanos = 0 ; // Not using System.nanoTime() since it is 1.5 specific
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public static Thread executeInThread(ThreadGroup group, Runnable run, String name, boolean asDaemon) {
    Thread t = new Thread(group, run, "Single Task Executor: " + name);
    t.setDaemon(asDaemon);
    t.start();
    return t;
  }

  public static Throwable getRootCause(Throwable t) {
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }

  public static  <R extends Throwable> R getRootCause(Throwable t, Class<? extends R> type) {
    while (t != null) {
      if (type.isInstance(t)) {
        return type.cast(t);
      }
      t = t.getCause();
    }
    return type.cast(t);
  }
}
