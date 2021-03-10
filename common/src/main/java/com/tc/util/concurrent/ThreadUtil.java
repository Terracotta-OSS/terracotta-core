/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util.concurrent;

import java.util.concurrent.CompletableFuture;
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

  public static Thread executeInThread(Runnable run, String name, boolean asDaemon) {
    Thread t = new Thread(run, "Single Task Executor: " + name);
    t.setDaemon(asDaemon);
    t.start();
    return t;
  }
}
