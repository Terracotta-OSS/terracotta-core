/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import java.util.Iterator;
import java.util.List;

/**
 * Test for CDV-138
 * 
 * @author hhuynh
 */
public class MassCloneTestApp extends AbstractErrorCatchingTransparentApp {
  private static final int COUNT = 6000;
  private List             root  = new ArrayList();
  private CyclicBarrier    barrier;

  public MassCloneTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  protected void runTest() throws Throwable {
    if (barrier.barrier() == 0) {
      System.err.println("creating " + COUNT + " objects...");
      synchronized (root) {
        for (int i = 0; i < COUNT; i++) {
          root.add(new MyStuff());
        }
      }
    }

    barrier.barrier();
    validateClone();
  }

  private void validateClone() {
    synchronized (root) {
      System.err.println("validating clones....");
      for (Iterator it = root.iterator(); it.hasNext();) {
        MyStuff cloned = (MyStuff) ((MyStuff) it.next()).clone();
        Assert.assertTrue(cloned.isSet());
        cloned = null;
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MassCloneTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(MyStuff.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");

    spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
  }

  private static class MyStuff {
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

    private MyStuff(int foo) {
      // just here to not initialize any fields
    }

    public boolean isSet() {
      return array != null && list != null;
    }

    protected Object clone() {
      MyStuff cloned = new MyStuff(0);
      cloned.array = (Object[]) array.clone();
      cloned.list = (List) ((ArrayList) list).clone();
      return cloned;
    }
  }

}
