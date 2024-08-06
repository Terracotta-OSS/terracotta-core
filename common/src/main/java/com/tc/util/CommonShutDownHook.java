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
package com.tc.util;

import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.List;

public class CommonShutDownHook implements Runnable {
  private static final SetOnceFlag run       = new SetOnceFlag();
  private static final List<Runnable>        runnables = new ArrayList<Runnable>();
  private static Thread            hooker;                       // ;-)
  private static boolean           shutdown;

  public static void shutdown() {
    synchronized (runnables) {
      if (shutdown) return;
      shutdown = true;

      runHooks();

      if (hooker != null) {
        try {
          Runtime.getRuntime().removeShutdownHook(hooker);
        } catch (IllegalStateException ise) {
          // expected if we're entering this code path from another shutdown hook
        } finally {
          hooker = null;
          runnables.clear();
        }
      }
    }
  }

  public static void addShutdownHook(Runnable r) {
    if (r == null) { throw new NullPointerException("Shutdown hook cannot be null"); }

    synchronized (runnables) {
      if (shutdown) throw new IllegalStateException("shutdown");

      runnables.add(r);

      if (hooker == null) {
        hooker = new Thread(new CommonShutDownHook(), "CommonShutDownHook");
        hooker.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(hooker);
      }
    }
  }

  @Override
  public void run() {
    runHooks();
  }

  private static void runHooks() {
    if (!run.attemptSet()) return;

    // Use a copy of the hooks for good measure (to avoid a possible ConcurrentModificationException here)
    final Runnable[] hooks;
    synchronized (runnables) {
      hooks = runnables.toArray(new Runnable[runnables.size()]);
    }

    for (Runnable r : hooks) {
      try {
        r.run();
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

}
