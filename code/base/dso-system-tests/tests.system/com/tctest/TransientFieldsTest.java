/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class TransientFieldsTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      TransparencyClassSpec spec = config.getOrCreateSpec(Foo.class.getName());
      spec.setHonorTransient(true);
      config.addTransient(Foo.class.getName(), "declaredTransientInt");
      config.addTransient(Foo.class.getName(), "declaredTransientObject");

      config.addRoot(new Root(App.class.getName(), "root", "root"), false);
      config.addRoot(new Root(App.class.getName(), "barrier", "barrier"), false);

      new CyclicBarrierSpec().visit(visitor, config);
    }

    private final Foo           root = new Foo();
    private final CyclicBarrier barrier;

    protected void runTest() throws Throwable {
      final int myId = Integer.parseInt(getApplicationId());
      final Object myObject = getApplicationId();

      // make sure our local id values don't happen to be java's defaults for uninitialized fields
      assertNotNull(myObject);
      assertFalse(myId == 0);

      // observe the transient fields as uninitialized
      assertEquals(0, root.getDeclaredTransientInt());
      assertEquals(0, root.getHonoredTransientInt());
      assertEquals(null, root.getDeclaredTransientObject());
      assertEquals(null, root.getHonoredTransientObject());

      barrier.barrier();

      // set the transient fields to local values
      root.setDeclaredTransientInt(myId);
      root.setHonoredTransientInt(myId);
      root.setDeclaredTransientObject(myObject);
      root.setHonoredTransientObject(myObject);

      barrier.barrier();

      // observe the the transient fields values are still what we expect them to be
      assertEquals(myId, root.getDeclaredTransientInt());
      assertEquals(myId, root.getHonoredTransientInt());
      assertEquals(myObject, root.getDeclaredTransientObject());
      assertEquals(myObject, root.getHonoredTransientObject());

      barrier.barrier();

      // make sure reference clearing does not disturb transient fields
      ((Manageable)root).__tc_managed().clearReferences(10000);
      assertEquals(myObject, root.getDeclaredTransientObject());
      assertEquals(myObject, root.getHonoredTransientObject());
    }
  }

  public static class Foo {
    private transient int    honoredTransientInt;
    private transient Object honoredTransientObject;

    private int              declaredTransientInt;
    private Object           declaredTransientObject;

    public int getHonoredTransientInt() {
      return honoredTransientInt;
    }

    public void setHonoredTransientInt(int honoredTransientInt) {
      this.honoredTransientInt = honoredTransientInt;
    }

    public Object getHonoredTransientObject() {
      return honoredTransientObject;
    }

    public void setHonoredTransientObject(Object honoredTransientObject) {
      this.honoredTransientObject = honoredTransientObject;
    }

    public int getDeclaredTransientInt() {
      return declaredTransientInt;
    }

    public void setDeclaredTransientInt(int declaredTransientInt) {
      this.declaredTransientInt = declaredTransientInt;
    }

    public Object getDeclaredTransientObject() {
      return declaredTransientObject;
    }

    public void setDeclaredTransientObject(Object declaredTransientObject) {
      this.declaredTransientObject = declaredTransientObject;
    }

  }

}
