/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.List;

public class CommonShutDownHook implements Runnable {
  private static final List runnables = new ArrayList();
  private static Thread     hooker;                     // ;-)
  private static boolean    shutdown;

  public static void shutdown(boolean runHooks) {
    synchronized (runnables) {
      if (shutdown) return;
      shutdown = true;
    }

    if (hooker != null) {
      try {
        Runtime.getRuntime().removeShutdownHook(hooker);
      } finally {
        try {
          hooker.start();
          hooker.join();
        } catch (InterruptedException e) {
          //
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

  public void run() {
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
