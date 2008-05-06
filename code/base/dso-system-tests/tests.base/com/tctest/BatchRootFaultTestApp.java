/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashSet;
import java.util.Set;

public class BatchRootFaultTestApp extends AbstractTransparentApp {
  private TestRoot root1;
  private TestRoot root2;
  private Set      nodes = new HashSet();

  public BatchRootFaultTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {

    synchronized (nodes) {

      if (nodes.size() == 0) {
        createBigRoot();
      } else {
        long l = System.currentTimeMillis();
        int count = 0;
        TestRoot current = root1;
        while (current != null) {
          count++;
          current = current.getNext();
        }
        current = root2;
        while (current != null) {
          count++;
          current = current.getNext();
        }
        System.out.println("******Took******:" + (System.currentTimeMillis() - l) + " count:" + count);
      }
      nodes.add(new Object());
    }
  }

  private void createBigRoot() {
    root1 = new TestRoot();
    TestRoot current = root1;
    for (int i = 0; i < 1000; i++) {
      current.setNext(new TestRoot());
      current = current.getNext();
    }
    root2 = new TestRoot();
    root2.setNext(root1);
  }

  private static class TestRoot {
    private TestRoot next;

    public void setNext(TestRoot m) {
      this.next = m;
    }

    public TestRoot getNext() {
      return this.next;
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = BatchRootFaultTestApp.class.getName();
    config.getOrCreateSpec(TestRoot.class.getName());
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root1", "root1");
    spec.addRoot("root2", "root2");
    spec.addRoot("nodes", "nodes");
  }
}
