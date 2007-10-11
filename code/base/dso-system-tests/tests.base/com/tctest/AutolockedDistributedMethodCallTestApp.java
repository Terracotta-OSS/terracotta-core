/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

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
    sharedObject = new SharedObject();
  }

  public void run() {
    try {
      int id = barrier.barrier();
      for (int i = 0; i < ITERATION_COUNT; i++) {
        if (id % nodeCount == 0) {
          System.out.println("##### appId=[" + appId + "] initiating autolockedSynchronizedIncrement method call");
          this.sharedObject.autolockedSynchronizedIncrement();
          System.out.println("##### appId=[" + appId + "] initiating autosynchronizedIncrement method call");
          this.sharedObject.autosynchronizedIncrement();
          System.out.println("##### appId=[" + appId + "] initiating autolockedSynchronizeBlockIncrement method call");
          this.sharedObject.autolockedSynchronizeBlockIncrement();
        }
      }
      barrier.barrier();
      int autolockedSynchronizedCount = this.sharedObject.getAutolockedSynchronizedCounter();
      int autosynchronizedCount = this.sharedObject.getAutosynchronizedCounter();
      int autolockedSynchronizeBlockCount = this.sharedObject.getAutolockedSynchronizeBlockCounter();
      int expectedCount = ITERATION_COUNT * nodeCount;

      Assert.assertEquals(expectedCount, autolockedSynchronizedCount);
      Assert.assertEquals(expectedCount, autosynchronizedCount);
      Assert.assertEquals(expectedCount, autolockedSynchronizeBlockCount);

    } catch (Throwable e) {
      notifyError(e);
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
      methodExpression = "* " + SharedObject.class.getName() + "*.get*(..)";
      config.addWriteAutolock(methodExpression);
      methodExpression = "* " + SharedObject.class.getName() + "*.autolockedSynchronizedIncrement(..)";
      config.addWriteAutolock(methodExpression);
      methodExpression = "* " + SharedObject.class.getName() + "*.autosynchronizedIncrement(..)";
      config.addWriteAutoSynchronize(methodExpression);
      methodExpression = "* " + SharedObject.class.getName() + "*.autolockedSynchronizeBlockIncrement(..)";
      config.addWriteAutolock(methodExpression);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private static class SharedObject {
    private int autolockedSynchronizedCounter;
    private int autosynchronizedCounter;
    private int autolockedSynchronizeBlockCounter;

    public SharedObject() {
      autolockedSynchronizedCounter = 0;
      autosynchronizedCounter = 0;
      autolockedSynchronizeBlockCounter = 0;
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
        autosynchronizedCounter++;
        System.out.println("##### appId=[" + AutolockedDistributedMethodCallTestApp.appId
                           + "] autolockedSynchronizeBlockIncrement called:  autolockedSynchronizeBlockCounter=["
                           + autolockedSynchronizeBlockCounter + "]");
      }
    }
  }

}
