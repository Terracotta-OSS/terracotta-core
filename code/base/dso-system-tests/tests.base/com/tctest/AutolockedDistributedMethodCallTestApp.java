/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public class AutolockedDistributedMethodCallTestApp extends AbstractTransparentApp {

  private static final int ITERATION_COUNT = 10;

  private static String    appId;
  private final int        nodeCount;

  // roots
  private CyclicBarrier    barrier;
  private SharedObject     sharedObject;

  public AutolockedDistributedMethodCallTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    AutolockedDistributedMethodCallTestApp.appId = appId;
    nodeCount = cfg.getGlobalParticipantCount();
    barrier = new CyclicBarrier(this.nodeCount);
    sharedObject = new SharedObject(nodeCount);
  }

  public void run() {
    try {
      int id = barrier.barrier();
      if (id % nodeCount == 0) {
        System.out.println("##### appId=[" + appId + "] initiating autolockedSynchronizedRead method call");
        this.sharedObject.autolockedSynchronizedRead();
        for (int i = 0; i < ITERATION_COUNT; i++) {
          System.out.println("##### appId=[" + appId + "] initiating autolockedSynchronizedIncrement method call");
          this.sharedObject.autolockedSynchronizedIncrement();
          System.out.println("##### appId=[" + appId + "] initiating autosynchronizedIncrement method call");
          this.sharedObject.autosynchronizedIncrement();
          System.out.println("##### appId=[" + appId + "] initiating autolockedSynchronizeBlockIncrement method call");
          this.sharedObject.autolockedSynchronizeBlockIncrement();
          System.out.println("##### appId=[" + appId + "] initiating localCounterIncrement method call");
          this.sharedObject.localCounterIncrement();
        }
      }
      barrier.barrier();

      waitForAllDistributedCalls();

      int autolockedSynchronizedCount = this.sharedObject.getAutolockedSynchronizedCounter();
      int autosynchronizedCount = this.sharedObject.getAutosynchronizedCounter();
      int autolockedSynchronizeBlockCount = this.sharedObject.getAutolockedSynchronizeBlockCounter();
      int localCount = this.sharedObject.getLocalCounter();
      int expectedCount = ITERATION_COUNT * nodeCount;

      Assert.assertEquals(expectedCount, autolockedSynchronizedCount);
      Assert.assertEquals(expectedCount, autosynchronizedCount);
      Assert.assertEquals(expectedCount, autolockedSynchronizeBlockCount);
      Assert.assertEquals(ITERATION_COUNT, localCount);

    } catch (Throwable e) {
      notifyError(e);
    }
  }

  private void waitForAllDistributedCalls() throws InterruptedException {
    final long waitDuration = 1000 * 15;
    long start = System.currentTimeMillis();
    while ((System.currentTimeMillis() - start) < waitDuration) {
      Thread.sleep(1000);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    try {
      new CyclicBarrierSpec().visit(visitor, config);

      String testClassName = AutolockedDistributedMethodCallTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("barrier", "barrier");
      spec.addRoot("sharedObject", "sharedObject");
      String methodExpression = "* " + testClassName + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec = config.getOrCreateSpec(SharedObject.class.getName());
      spec.addDistributedMethodCall("autolockedSynchronizedIncrement", "()V", true);
      spec.addDistributedMethodCall("autosynchronizedIncrement", "()V", true);
      spec.addDistributedMethodCall("autolockedSynchronizeBlockIncrement", "()V", true);
      spec.addDistributedMethodCall("localCounterIncrement", "()V", true);
      spec.addDistributedMethodCall("autolockedSynchronizedRead", "()V", true);
      spec.addTransient("localObject");
      spec.addTransient("localCounter");
      methodExpression = "* " + SharedObject.class.getName() + "*.get*(..)";
      config.addWriteAutolock(methodExpression);
      methodExpression = "* " + SharedObject.class.getName() + "*.autolockedSynchronizedIncrement(..)";
      config.addWriteAutolock(methodExpression);
      methodExpression = "* " + SharedObject.class.getName() + "*.autosynchronizedIncrement(..)";
      config.addWriteAutoSynchronize(methodExpression);
      methodExpression = "* " + SharedObject.class.getName() + "*.autolockedSynchronizeBlockIncrement(..)";
      config.addWriteAutolock(methodExpression);
      methodExpression = "* " + SharedObject.class.getName() + "*.autolockedSynchronizedRead(..)";
      config.addReadAutolock(methodExpression);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private static class SharedObject {
    private static final Object localObject  = new Object();
    private static int          localCounter = 0;

    private int                 autolockedSynchronizedCounter;
    private int                 autosynchronizedCounter;
    private int                 autolockedSynchronizeBlockCounter;
    private CyclicBarrier       readerBarrier;

    public SharedObject(int readerCount) {
      autolockedSynchronizedCounter = 0;
      autosynchronizedCounter = 0;
      autolockedSynchronizeBlockCounter = 0;
      readerBarrier = new CyclicBarrier(readerCount);
    }

    public synchronized int getAutolockedSynchronizedCounter() {
      return autolockedSynchronizedCounter;
    }

    public synchronized int getAutosynchronizedCounter() {
      return autosynchronizedCounter;
    }

    public synchronized int getAutolockedSynchronizeBlockCounter() {
      return autolockedSynchronizeBlockCounter;
    }

    public synchronized void autolockedSynchronizedIncrement() {
      autolockedSynchronizedCounter++;
      System.out.println("##### appId=[" + AutolockedDistributedMethodCallTestApp.appId
                         + "] autolockedSynchronizedIncrement called:  autolockedSynchronizedCounter=["
                         + autolockedSynchronizedCounter + "]");
    }

    public void autosynchronizedIncrement() {
      autosynchronizedCounter++;
      System.out.println("##### appId=[" + AutolockedDistributedMethodCallTestApp.appId
                         + "] autosynchronizedIncrement called:  autosynchronizedCounter=[" + autosynchronizedCounter
                         + "]");
    }

    public void autolockedSynchronizeBlockIncrement() {
      synchronized (this) {
        autolockedSynchronizeBlockCounter++;
        System.out.println("##### appId=[" + AutolockedDistributedMethodCallTestApp.appId
                           + "] autolockedSynchronizeBlockIncrement called:  autolockedSynchronizeBlockCounter=["
                           + autolockedSynchronizeBlockCounter + "]");
      }
    }

    public void localCounterIncrement() {
      synchronized (localObject) {
        localCounter++;
        System.out.println("***** appId=[" + AutolockedDistributedMethodCallTestApp.appId
                           + "] localCounterIncrement called:  localCounter=[" + localCounter + "]");
      }
    }

    public int getLocalCounter() {
      synchronized (localObject) {
        return localCounter;
      }
    }

    public synchronized void autolockedSynchronizedRead() throws BrokenBarrierException, InterruptedException {
      System.out.println("***** appId=[" + AutolockedDistributedMethodCallTestApp.appId
                         + "] autolockedSynchronizedRead called:  autolockedSynchronizedCounter=["
                         + autolockedSynchronizedCounter + "]");
      readerBarrier.barrier();
    }
  }

}
