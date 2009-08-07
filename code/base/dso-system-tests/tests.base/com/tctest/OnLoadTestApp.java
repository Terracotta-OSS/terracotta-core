/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;

public class OnLoadTestApp extends AbstractErrorCatchingTransparentApp {
  private final CyclicBarrier barrier;

  private MyObject            root;
  private MyObject1           root1;
  private MyObject2           root2;
  private MyObject3           root3;

  public OnLoadTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  @Override
  protected void runTest() throws Throwable {
    // force the outcome of the race to create (as opposed to faulting) the roots
    boolean creator = (barrier.barrier() == 0);

    if (creator) {
      createRoots();
    }

    barrier.barrier();

    synchronized (root) {
      Assert.eval(root.getSize() == 0);
      Assert.eval(root.ok);

      Assert.eval(root1.getSize() == 0);

      try {
        root2.getSize();
        Assert.eval(creator);
      } catch (NullPointerException npe) {
        Assert.eval(!creator);
        // success
        System.out.println("YEAH! GOT NPE");
      }
    }

    // Test case to test for error in OnLoad script.
    synchronized (root3) {
      try {
        root3.getSize();
        Assert.eval(creator);
      } catch (NullPointerException npe) {
        Assert.eval(!creator);
        // success
        System.out.println("YEAH! GOT NPE");
      }
    }
  }

  private void createRoots() {
    root = new MyObject(null);
    root1 = new MyObject1(null);
    root2 = new MyObject2(null);
    root3 = new MyObject3();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    new CyclicBarrierSpec().visit(visitor, config);

    String testClass = OnLoadTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(MyObject.class.getName());
    spec.setHonorTransient(true);
    // We want to make sure that all objects are resolved.
    spec
        .setExecuteScriptOnLoad("if(self.mine == null){self.ok = false;}else{self.ok = true;} self.list = new ArrayList();");

    spec = config.getOrCreateSpec(MyObject1.class.getName());
    spec.setHonorTransient(true);
    spec.setCallMethodOnLoad("initialize");

    spec = config.getOrCreateSpec(MyObject2.class.getName());
    spec.setHonorTransient(true);

    spec = config.getOrCreateSpec(MyObject3.class.getName());
    spec.setHonorTransient(true);
    spec.setExecuteScriptOnLoad("<![CDATA[***********;]]>");

    spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("root1", "root1");
    spec.addRoot("root2", "root2");
    spec.addRoot("root3", "root3");
    spec.addRoot("barrier", "barrier");
  }

  private static class MyObject {
    private transient final List      list = new ArrayList();
    private final MyObject1           mine = new MyObject1(null);
    public volatile transient boolean ok   = true;

    public MyObject(Object o) {
      // I don't want a null constructor to exist so I created this.
      System.out.println(mine);
    }

    public int getSize() {
      return list.size();
    }

  }

  private static class MyObject1 {
    private transient List list;

    public MyObject1(Object o) {
      initialize();
    }

    public void initialize() {
      list = new ArrayList();
    }

    public int getSize() {
      return list.size();
    }
  }

  private static class MyObject2 {
    private transient final List list = new ArrayList();

    public MyObject2(Object o) {
      //
    }

    public int getSize() {
      return list.size();
    }
  }

  /**
   * This class is used for testing onLoad script with parsing error.
   */
  private static class MyObject3 {
    private transient final List list = new ArrayList();

    public MyObject3() {
      super();
    }

    public int getSize() {
      return list.size();
    }

  }
}
