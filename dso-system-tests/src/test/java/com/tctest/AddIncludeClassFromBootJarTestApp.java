/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class AddIncludeClassFromBootJarTestApp extends AbstractTransparentApp {
  private final CyclicBarrier          barrier;
  private final DataRoot               dataRoot = new DataRoot();

  private final ReentrantReadWriteLock RWL      = new ReentrantReadWriteLock();

  public AddIncludeClassFromBootJarTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      if (index == 0) {
        WriteLock writeLock = RWL.writeLock();
        writeLock.lock();
        try {
          dataRoot.setStr("test Str");
        } finally {
          writeLock.unlock();
        }
      }

      barrier.await();

      WriteLock writeLock = RWL.writeLock();
      writeLock.lock();
      try {
        Assert.assertEquals("test Str", dataRoot.getStr());
      } finally {
        writeLock.unlock();
      }
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = AddIncludeClassFromBootJarTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("RWL", "RWL");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("dataRoot", "dataRoot");

    // Add ReentrantReadWriteLock.WriteLock to the config again with honor transient set.
    String className = ReentrantReadWriteLock.WriteLock.class.getName();
    config.addIncludeAndLockIfRequired(className, true, true, false, "* " + className + ".*(..)", AsmClassInfo
        .getClassInfo(className, ReentrantReadWriteLock.class.getClassLoader()));
  }

  private static class DataRoot {
    private String str;

    public DataRoot() {
      super();
    }

    public void setStr(String str) {
      this.str = str;
    }

    public String getStr() {
      return this.str;
    }
  }

}
