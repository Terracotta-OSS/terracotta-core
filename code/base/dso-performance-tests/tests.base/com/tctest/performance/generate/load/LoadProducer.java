/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.generate.load;

import com.tc.util.Assert;
import com.tctest.performance.simulate.type.SimulatedType;

/**
 * @ThreadSafe
 * @LivenessTuned
 */
final class LoadProducer extends Thread {

  private final LoadBuffer    loadBuffer;
  private final SimulatedType factory;
  private final int           percentUnique;
  private final double        duration, minLoad, maxLoad;
  private long                counter;
  private boolean             started;

  LoadProducer(LoadBuffer loadBuffer, int duration, int minLoad, int maxLoad, SimulatedType factory, int percentUnique) {
    Assert.assertTrue(percentUnique >= 0);
    Assert.assertTrue(percentUnique <= 100);
    Assert.assertTrue(minLoad <= maxLoad);
    Assert.assertTrue(duration > 0);
    Assert.assertTrue(minLoad > 0);
    Assert.assertNotNull(factory);
    this.loadBuffer = loadBuffer;
    this.duration = duration;
    this.minLoad = minLoad;
    this.maxLoad = maxLoad;
    this.factory = factory;
    this.percentUnique = percentUnique;
    this.counter = minLoad;
    setDaemon(true);
    setPriority(Thread.MIN_PRIORITY);
  }

  public synchronized void run() {
    if (started) throw new IllegalStateException(this.getClass().getName() + " has already been started.");
    started = true;

    try {
      activateWorkers();
      for (int i = 1; i <= duration; i++) {
        // magic load formula
        setCounter(Math.round((((maxLoad - minLoad) / duration) * i) + minLoad));
        while (counter > 0)
          wait();
        if (!loadBuffer.put(factory.clone(percentUnique), true)) {
          setCounter(0);
          System.err
              .println("The CPU did not have the available resources to produce the maximum obj/sec specified by the load generator.");
          return;
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void activateWorkers() {
    new Worker().start();
  }

  private synchronized Object getWork() throws InterruptedException {
    while (counter < 1)
      wait();
    --counter;
    notifyAll();
    return factory.clone(percentUnique);
  }

  private synchronized void setCounter(long magnitude) {
    counter = magnitude;
    notifyAll();
  }

  private class Worker extends Thread {

    private Worker() {
      setDaemon(true);
    }

    public void run() {
      try {
        while (true) {
          loadBuffer.put(getWork(), false);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
