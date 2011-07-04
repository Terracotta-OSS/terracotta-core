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

import gnu.trove.THashSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TransparentTHashSetTestApp extends AbstractTransparentApp {

  private THashSet tsetroot = new THashSet();
  private Set      steps    = new HashSet();

  public TransparentTHashSetTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

  }

  public void run() {
    System.out.println("Running...");
    synchronized (tsetroot) {
      switch (steps.size()) {
        case 0:
          stage1();
          break;
        case 1:
          stage2();
          break;
      }
      steps.add(new Object());
      System.out.println("Stage:" + steps.size());
    }
  }

  private void stage2() {
    for (Iterator i = tsetroot.iterator(); i.hasNext();) {
      System.out.println(i.next());
    }
    Assert.assertEquals(1, tsetroot.size());
  }

  private void stage1() {
    TestObject to1 = new TestObject("1");
    tsetroot.add(to1);
    tsetroot.add(new TestObject("1"));
    tsetroot.add(new TestObject("4"));
    tsetroot.remove(new TestObject("4"));
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = TransparentTHashSetTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("tsetroot", "tsetroot");
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

    public String toString() {
      return value;
    }
  }
}
