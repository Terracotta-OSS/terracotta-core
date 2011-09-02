/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for CDV-138
 * 
 * @author hhuynh
 */
public class MassCloneTestApp extends AbstractErrorCatchingTransparentApp {
  private static final int COUNT    = 6000;
  private static final int RUN_TIME = 3 * 60 * 1000;

  private List             root     = new ArrayList();
  private CyclicBarrier    barrier;

  public MassCloneTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  protected void runTest() throws Throwable {
    if (barrier.barrier() == 0) {
      System.err.println("creating " + COUNT + " objects...");
      int batch = 20;
      for (int i = 0; i < COUNT; i += batch) {
        synchronized (root) {
          for (int j = 0; j < batch; j++) {
            root.add(new MyStuff());
          }
        }
      }
      System.err.println("created " + root.size() + " objects.");
    }
    barrier.barrier();

    System.err.println("Validating clondes...");
    long timeout = System.currentTimeMillis() + RUN_TIME;
    int index = 0;
    while (System.currentTimeMillis() < timeout) {
      synchronized (root) {
        MyStuff cloned = (MyStuff) ((MyStuff) root.get(index)).clone();
        Assert.assertTrue(cloned.allFieldsSet());
      }
      if (++index >= COUNT) {
        index = 0;
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MassCloneTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(MyStuff.class.getName());
    config.addWriteAutolock("* " + MyStuff.class.getName() + "*.*(..)");

    config.addWriteAutolock("* " + testClass + "*.runTest(..)");

    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");

    spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
  }

  private static class MyStuff implements Cloneable {
    public Object[] array;
    public List     list;

    public MyStuff() {
      int size = 2;
      array = new Object[size];
      list = new ArrayList();

      for (int i = 0; i < size; i++) {
        array[i] = new Object();
        list.add(new Object());
      }
    }

    public boolean allFieldsSet() {
      return array != null && list != null;
    }

    protected Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }

}
