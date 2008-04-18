/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.object.partitions.PartitionManager;

public class CacheInvalidationTimer {

  private final long     delayInSecs;
  private final String   timerName;
  private EvictionRunner runner;

  public CacheInvalidationTimer(final long delayInSecs, final String timerName) {

    this.delayInSecs = delayInSecs;
    this.timerName = timerName;
  }

  public void start(CacheEntryInvalidator evictor, int partitionId) {
    if (delayInSecs <= 0) { return; }

    this.runner = new EvictionRunner(delayInSecs, evictor, partitionId);
    Thread t = new Thread(runner, timerName);
    t.setDaemon(true);
    t.start();
  }

  public void stop() {
    if (runner != null) {
      runner.cancel();
    }
  }

  private static class EvictionRunner implements Runnable {
    private final long                            delayMillis;
    private volatile boolean                      running = true;
    private final transient CacheEntryInvalidator evictor;
    private final int                             partitionId;

    public EvictionRunner(final long delayInSecs, final CacheEntryInvalidator evictor, int partitionId) {
      this.evictor = evictor;
      this.delayMillis = delayInSecs * 1000;
      this.partitionId = partitionId;
    }

    public void cancel() {
      running = false;
    }

    public void run() {
      PartitionManager.setPartition(partitionId);
      long nextDelay = delayMillis;
      try {
        do {
          sleep(nextDelay);
          evictor.run();
        } while (running);
      } finally {
        evictor.postRun();
      }
    }

    private void sleep(long l) {
      try {
        Thread.sleep(l);
      } catch (InterruptedException ignore) {
        // nothing to do
      }
    }

  }
}