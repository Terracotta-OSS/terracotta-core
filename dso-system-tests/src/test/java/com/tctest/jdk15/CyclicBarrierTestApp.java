/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CyclicBarrierTestApp extends AbstractTransparentApp {
  private static final int    FINISHED_BARRIER_ACTION_FLAG = 100;

  private final DataRoot      dataRoot                     = new DataRoot();
  private final CyclicBarrier barrier;
  private final CyclicBarrier testBarrier;

  public CyclicBarrierTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    testBarrier = new CyclicBarrier(getParticipantCount(), new BarrierAction(dataRoot));
  }

  public void run() {
    try {
      basicBarrierActionTest();
      basicBarrierAPITest();
      interruptedBarrierTest();
      thrashingBarrierTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void clear() throws Exception {
    synchronized (dataRoot) {
      dataRoot.clear();
    }

    barrier.await();
  }

  private void basicBarrierActionTest() throws Exception {
    testBarrier.await();

    Assert.assertEquals(FINISHED_BARRIER_ACTION_FLAG, dataRoot.getData());

    barrier.await();
  }

  private void basicBarrierAPITest() throws Exception {
    Assert.assertEquals(getParticipantCount(), testBarrier.getParties());

    clear();

    int index = -1;
    synchronized (dataRoot) {
      index = dataRoot.getIndex();
      dataRoot.setIndex(index + 1);
    }

    barrier.await();

    if (index == 0) {
      try {
        Assert.assertEquals(0, testBarrier.getNumberWaiting());
        testBarrier.await(10, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        Assert.assertTrue(testBarrier.isBroken());
      }
    }

    barrier.await();

    if (index != 0) {
      Assert.assertTrue(testBarrier.isBroken());
    }

    barrier.await();

    if (index == 0) {
      testBarrier.reset();
    }

    barrier.await();

    Assert.assertFalse(testBarrier.isBroken());

    testBarrier.await();
  }

  private void interruptedBarrierTest() throws Exception {
    clear();

    int index = -1;
    synchronized (dataRoot) {
      index = dataRoot.getIndex();
      dataRoot.setIndex(index + 1);
    }

    barrier.await();

    Thread thread = null;

    if (index == 0) {
      thread = new Thread(new BarrierRunnable(testBarrier));
      thread.start();
    }

    if (index == 1) {
      while (testBarrier.getNumberWaiting() == 0) {
        // do nothing
      }
    }

    barrier.await();

    if (index == 0) {
      thread.interrupt();
    }

    barrier.await();

    if (index == 0) {
      thread.join();
    }

    barrier.await();
  }

  private void thrashingBarrierTest() throws Exception {    
    for (int i = 0; i < 1000; i++) {
      barrier.await();
    }
  }
  
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = CyclicBarrierTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("testBarrier", "testBarrier");
    spec.addRoot("dataRoot", "dataRoot");
  }

  private static class BarrierAction implements Runnable {
    private final DataRoot dataRoot;

    public BarrierAction(DataRoot dataRoot) {
      this.dataRoot = dataRoot;
    }

    public void run() {
      dataRoot.setData(FINISHED_BARRIER_ACTION_FLAG);
    }
  }

  private class BarrierRunnable implements Runnable {
    private CyclicBarrier b;

    public BarrierRunnable(CyclicBarrier b) {
      this.b = b;
    }

    public void run() {
      try {
        run0();
      } catch (Throwable t) {
        notifyError(t);
      }
    }

    private void run0() {
      try {
        b.await();
      } catch (InterruptedException e) {
        Assert.assertTrue(b.isBroken());
      } catch (BrokenBarrierException e) {
        Assert.assertTrue(b.isBroken());
      }
    }
  }

  private static class DataRoot {
    private int          index;
    private volatile int data;

    public DataRoot() {
      this.index = 0;
    }

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public int getData() {
      return data;
    }

    public void setData(int data) {
      this.data = data;
    }

    public void clear() {
      this.index = 0;
      this.data = 0;
    }
  }
}
