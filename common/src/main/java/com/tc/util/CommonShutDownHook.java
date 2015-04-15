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

import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.List;

public class CommonShutDownHook implements Runnable {
  private static final SetOnceFlag run       = new SetOnceFlag();
  private static final List        runnables = new ArrayList();
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
      hooks = (Runnable[]) runnables.toArray(new Runnable[runnables.size()]);
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
