/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for CDV-190 - auto-synchronize feature
 * 
 * @author hhuynh
 */
public class AutoSynchTestApp extends AbstractErrorCatchingTransparentApp {
  private final CyclicBarrier barrier;
  private BaseClass           root = new BaseClass();

  public AutoSynchTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, com.tc.object.config.DSOClientConfigHelper config) {
    String testClass = AutoSynchTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
    new CyclicBarrierSpec().visit(visitor, config);

    String baseClass = BaseClass.class.getName();
    config.addIncludePattern(baseClass);
    config.addAutoSynchronize("* " + baseClass + ".add(..)", ConfigLockLevel.WRITE);
    config.addAutoSynchronize("* " + baseClass + ".getSize(..)", ConfigLockLevel.READ);
  }

  protected void runTest() throws Throwable {
    try {
      root.add("one");
      root.add("two");
    } catch (Throwable t) {
      t.printStackTrace();
    }

    barrier.barrier();
    Assert.assertEquals(4, root.getSize());
  }

  static class BaseClass {
    protected List list = new ArrayList();

    public void add(Object o) {
      // intentionally not using synchronize
      //ManagerUtil.monitorEnter(list, Manager.LOCK_TYPE_WRITE);
      list.add(o);
      //ManagerUtil.monitorExit(list);
    }

    public int getSize() {
      // intentionally not using synchronize
      return list.size();
    }

  }

}
