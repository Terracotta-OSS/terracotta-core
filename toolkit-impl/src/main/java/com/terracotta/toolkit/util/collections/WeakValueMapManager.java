/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util.collections;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WeakValueMapManager {
  private final CleanerRunnable    cleanerRunnable;
  private final List<WeakValueMap> weakMaps               = new CopyOnWriteArrayList<WeakValueMap>();
  private volatile boolean         interruptedOrCancelled = false;

  public WeakValueMapManager() {
    this.cleanerRunnable = new CleanerRunnable();
    Thread t = new Thread(cleanerRunnable, "Toolkit Weak Maps Cleaner Thread");
    t.setDaemon(true);
    t.start();
  }

  public <V> WeakValueMap<V> createWeakValueMap() {
    if (interruptedOrCancelled) { throw new IllegalStateException("Cannot create maps after cancelled"); }

    WeakValueMap<V> map = new WeakValueMap<V>();
    weakMaps.add(map);

    return map;
  }

  public void cancel() {
    interruptedOrCancelled = true;
  }

  private class CleanerRunnable implements Runnable {
    private static final int WAIT_TIME = 1000;

    @Override
    public void run() {
      while (!interruptedOrCancelled) {
        for (WeakValueMap valueMap : weakMaps) {
          valueMap.cleanupReferenceQueue();
        }

        sleepForSomeTime(WAIT_TIME);
      }
    }

    private void sleepForSomeTime(int waitTime) {
      try {
        Thread.sleep(waitTime);
      } catch (InterruptedException e) {
        interruptedOrCancelled = true;
        Thread.currentThread().interrupt();
      }
    }
  }
}
