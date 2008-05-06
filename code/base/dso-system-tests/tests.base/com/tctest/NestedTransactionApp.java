/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NestedTransactionApp extends AbstractTransparentApp {

  private static final String TARGET_CLASS_NAME       = "com.tctest.NestedTransactionApp";
  private static final String TARGET_INNER_CLASS_NAME = "com.tctest.NestedTransactionApp$TestObj";

  private final static int    ACTIONS                 = 20;
  public final static int     NODE_COUNT              = 3;

  private int                 myCount;
  private int                 totalCount;

  private final List          list1;
  private List                list2                   = new ArrayList();
  private List                list3                   = new ArrayList();

  public NestedTransactionApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.list1 = new LinkedList();
    this.myCount = ACTIONS;
    this.totalCount = ACTIONS * NODE_COUNT;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(TARGET_CLASS_NAME);

    config.getOrCreateSpec(TARGET_INNER_CLASS_NAME);

    spec.addRoot("list1", "stuff1");
    spec.addRoot("list2", "stuff2");
    spec.addRoot("list3", "stuff3");

    String methodPattern = "* " + TARGET_CLASS_NAME + ".add1(..)";
    config.addWriteAutolock(methodPattern);

    methodPattern = "* " + TARGET_CLASS_NAME + ".add2(..)";
    config.addWriteAutolock(methodPattern);

    methodPattern = "* " + TARGET_CLASS_NAME + ".add3(..)";
    config.addWriteAutolock(methodPattern);

    methodPattern = "* " + TARGET_CLASS_NAME + ".move(..)";
    config.addWriteAutolock(methodPattern);

    methodPattern = "* " + TARGET_CLASS_NAME + ".remove1(..)";
    config.addWriteAutolock(methodPattern);

    methodPattern = "* " + TARGET_CLASS_NAME + ".remove2(..)";
    config.addWriteAutolock(methodPattern);

    methodPattern = "* " + TARGET_CLASS_NAME + ".finished(..)";
    config.addWriteAutolock(methodPattern);

    methodPattern = "* " + TARGET_CLASS_NAME + ".notDone(..)";
    config.addWriteAutolock(methodPattern);

  }

  public void add1(TestObj testObj) {
    synchronized (list1) {
      int s = list1.size();
      list1.add(testObj);
      Assert.eval(s + 1 == list1.size());
      // System.out.println("Added1:"+list1.size());
    }
  }

  public void add3(TestObj testObj) {
    synchronized (list3) {
      int s = list3.size();
      list3.add(testObj);
      Assert.eval(s + 1 == list3.size());
      // System.out.println("Added1:"+list1.size());
    }
  }

  public void add2(TestObj testObj) {
    synchronized (list2) {
      try {
        int s = list2.size();
        list2.add(testObj);
        Assert.eval(s + 1 == list2.size());
        // System.out.println("Added2:"+list2.size());
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }

  public void move() {
    synchronized (list1) {
      synchronized (list2) {
        try {
          if (list1.size() > 0) {
            // System.out.println("Moving from 1 to 2");
            int s = list1.size();
            TestObj obj = remove1();
            // Assert.eval(s - 1 == list1.size());
            if (s - 1 != list1.size()) { throw new AssertionError("s - 1 (" + (s - 1) + ") != list1.size() ("
                                                                  + list1.size() + ")"); }
            // System.out.println("list1 size:"+list1.size());
            add2(obj);
          }
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
    }
  }

  public TestObj remove1() {
    TestObj to = null;
    try {
      int s = list1.size();
      if (s > 0) {
        to = (TestObj) list1.remove(0);
        if (s - 1 != list1.size()) { throw new AssertionError("s - 1 (" + (s - 1) + ") != list1.size() ("
                                                              + list1.size() + ")"); }
        // Assert.eval(s - 1 == list1.size());
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return to;

  }

  public void run() {
    add3(new TestObj());

    for (int i = 0; i < myCount; i++) {
      add1(new TestObj());
    }

    while (notDone()) {
      ThreadUtil.reallySleep(50);
      move();
    }
    finished();
  }

  private void finished() {
    synchronized (list2) {
      if (list2.size() != totalCount) { throw new AssertionError("list2.size()=" + list2.size() + ", expected: "
                                                                 + totalCount); }
    }
  }

  private boolean notDone() {
    synchronized (list2) {
      if (list2.size() > totalCount - 10) System.err.println("list2.size()=" + list2.size() + " still less than "
                                                             + totalCount + ".  NOT DONE. list1.size()=" + list1.size()
                                                             + ", list3.size()=" + list3.size());
      return list2.size() < totalCount;
    }
  }

  private class TestObj {
    //
  }
}