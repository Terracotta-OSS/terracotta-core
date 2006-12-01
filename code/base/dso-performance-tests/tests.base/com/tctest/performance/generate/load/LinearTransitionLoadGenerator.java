/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.generate.load;

import com.tc.util.Assert;
import com.tctest.performance.simulate.type.SimulatedType;

/**
 * @ThreadSafe
 */
public final class LinearTransitionLoadGenerator implements LoadGenerator {

  private static final int   BUFFER_SIZE_SECONDS    = 10;
  private static final int   WORKQUEUE_SIZE_SECONDS = 10;
  private volatile boolean   started;
  private MonitoredWorkQueue workQueue;
  private Measurement[]      waitTimes;

  public synchronized void start(int duration, int minLoad, int maxLoad, SimulatedType factory, int percentUnique) {
    Assert.assertTrue(!started);
    started = true;
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

    LoadBuffer buffer = new LoadBuffer(maxLoad * BUFFER_SIZE_SECONDS);
    workQueue = new MonitoredWorkQueue(maxLoad * WORKQUEUE_SIZE_SECONDS, true);

    LoadProducer producer = new LoadProducer(buffer, duration, minLoad, maxLoad, factory, percentUnique);
    LoadConsumer consumer = new LoadConsumer(buffer, workQueue);

    producer.start();
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    consumer.start();
  }

  public Object getNext() throws InterruptedException, WorkQueueOverflowException {
    Assert.assertTrue(started);
    try {
      Object obj = workQueue.get();
      if (obj == null) waitTimes = workQueue.getWaitTimes();
      return obj;
    } catch (WorkQueueOverflowException e) {
      waitTimes = workQueue.getWaitTimes();
      throw e;
    }
  }

  public Measurement[] getWaitTimes() {
    return waitTimes;
  }
}
