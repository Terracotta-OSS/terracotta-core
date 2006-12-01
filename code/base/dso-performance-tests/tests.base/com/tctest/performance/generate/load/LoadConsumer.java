/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.generate.load;

import com.tc.util.Assert;

/**
 * @ThreadSafe
 * @LivenessTuned
 */
final class LoadConsumer extends Thread {

  private final LoadBuffer         loadBuffer;
  private final MonitoredWorkQueue workQueue;

  LoadConsumer(LoadBuffer loadBuffer, MonitoredWorkQueue workQueue) {
    Assert.assertNotNull(loadBuffer);
    Assert.assertNotNull(workQueue);
    this.loadBuffer = loadBuffer;
    this.workQueue = workQueue;
    setDaemon(true);
    setPriority(Thread.MIN_PRIORITY);
  }

  public void run() {
    Thread worker1 = new Worker();
    Thread worker2 = new Worker();
    worker1.start();
    worker2.start();
    try {
      worker1.join();
      worker2.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private class Worker extends Thread {

    private Worker() {
      setDaemon(true);
      setPriority(Thread.MIN_PRIORITY);
    }

    public void run() {
      try {
        while (true) {
          workQueue.put(loadBuffer.get());
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
