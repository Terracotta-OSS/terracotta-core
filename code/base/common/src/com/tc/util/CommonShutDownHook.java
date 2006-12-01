/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.List;

public class CommonShutDownHook implements Runnable {
  private static final List runnables = new ArrayList();
  private static Thread     hooker;                     // ;-)

  public static void addShutdownHook(Runnable r) {
    if (r == null) { throw new NullPointerException("Shutdown hook cannot be null"); }
    synchronized (runnables) {
      runnables.add(r);

      if (hooker == null) {
        hooker = new Thread(new CommonShutDownHook());
        hooker.setName("CommonShutDownHook");
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

    for (int i = 0; i < hooks.length; i++) {
      Runnable r = hooks[i];
      Thread.currentThread().setName("CommonShutDownHook - " + r);

      try {
        r.run();
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }
}
