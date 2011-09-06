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
import com.tctest.runner.AbstractTransparentApp;

import gnu.trove.THashMap;
import gnu.trove.TObjectObjectProcedure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransparentTHashMapTestApp extends AbstractTransparentApp {
  private THashMap tmaproot = new THashMap();
  private Set      steps    = new HashSet();

  public TransparentTHashMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

  }

  public void run() {
    System.out.println("Running...");
    synchronized (tmaproot) {
      switch (steps.size()) {
        case 0:
          stage1();
          break;
        case 1:
          stage2();
          break;
        case 2:
          Assert.eval(tmaproot.isEmpty());
          break;
        case 3:
          stage3();
          break;
        case 4:
          stage4();
          break;
        case 5:
          stage5();
          tmaproot.put("DONE", "DONE");
          tmaproot.notifyAll();
          break;
      }

      steps.add(new Object());
      System.out.println("Stage: " + steps.size());

      // This bit of wierdness is to make sure this test is actually running all the stages
      while (!tmaproot.containsKey("DONE")) {
        try {
          tmaproot.wait();
        } catch (InterruptedException e) {
          notifyError(e);
        }
      }
    }
  }

  private void stage5() {
    Assert.eval(tmaproot.get("hello").equals(new TestObject("6")));
    Assert.eval(tmaproot.keySet().size() == 1);
  }

  private void stage2() {
    // System.out.println("Size: " + tmaproot.keySet().size());
    Assert.eval(tmaproot.keySet().size() == 1);
    Assert.eval(tmaproot.get(new TestObject("1")).equals(new TestObject("3")));
    Assert.eval(tmaproot.get(new TestObject("4")) == null);
    tmaproot.clear();
  }

  private void stage3() {
    Map tm = new HashMap();
    tm.put("hello", new TestObject("6"));
    tm.put(new TestObject("7"), new TestObject("8"));
    tmaproot.putAll(tm);
  }

  private void stage4() {
    tmaproot.retainEntries(new TObjectObjectProcedure() {

      public boolean execute(Object arg0, Object arg1) {
        return (arg0.equals("hello"));
      }
    });
  }

  private void stage1() {
    TestObject to1 = new TestObject("1");
    TestObject to2 = new TestObject("2");
    tmaproot.put(to1, to2);
    tmaproot.put(new TestObject("1"), new TestObject("3"));
    tmaproot.put(new TestObject("4"), new TestObject("5"));
    tmaproot.remove(new TestObject("4"));
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = TransparentTHashMapTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("tmaproot", "tmaproot");
    spec.addRoot("steps", "steps");
    
    config.addIncludePattern(TestObject.class.getName());
  }

  private static class TestObject {
    private String value;

    public TestObject(String value) {
      this.value = value;
    }

    public int hashCode() {
      return value.hashCode();
    }

    public boolean equals(Object obj) {
      if (obj instanceof TestObject) {
        TestObject to = (TestObject) obj;
        return this.value.equals(to.value);
      }
      return false;
    }
  }
}
