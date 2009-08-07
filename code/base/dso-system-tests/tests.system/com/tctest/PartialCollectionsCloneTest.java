/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PartialCollectionsCloneTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return PartialCollectionsCloneTestApp.class;
  }

  public static class PartialCollectionsCloneTestApp extends AbstractErrorCatchingTransparentApp {
    private static final int    MAP_SIZE = 1000;
    private final CyclicBarrier barrier;
    private HashMap             hashmap;
    private Hashtable           hashtable;

    public PartialCollectionsCloneTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    @Override
    protected void runTest() throws Throwable {
      boolean rootCreator = barrier.barrier() == 0;

      if (rootCreator) {
        createRoots();
      }

      barrier.barrier();

      if (!rootCreator) {
        testMapClone(hashmap);
        testMapClone(hashtable);
      }

      barrier.barrier();
    }

    private void testMapClone(Map m) {
      final Map cloned;

      // read the map in the same lock under which is was mutated
      synchronized (m) {
        cloned = (Map) cloneIt(m);
      }

      Assert.assertEquals(MAP_SIZE, cloned.size());

      if (((Manageable) cloned).__tc_isManaged()) { throw new AssertionError("cloned object is shared"); }

      for (Iterator i = cloned.entrySet().iterator(); i.hasNext();) {
        Map.Entry entry = (Entry) i.next();

        // Before the fix for clone() of partial maps, this would fail with ClassCastException since unresolved
        // types are still in the map values
        NonLiteralObject nlo = (NonLiteralObject) entry.getValue();
        Assert.assertNotNull(nlo);
      }
    }

    private Object cloneIt(Object o) {
      try {
        Method m = o.getClass().getDeclaredMethod("clone", new Class[] {});
        return m.invoke(o, new Object[] {});
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private void createRoots() {
      hashmap = new HashMap();
      hashtable = new Hashtable();

      populateMap(hashmap);
      populateMap(hashtable);
    }

    private void populateMap(Map m) {
      synchronized (m) {
        for (int i = 0; i < MAP_SIZE; i++) {
          m.put(new Integer(i), new NonLiteralObject());
        }
      }
    }

    private static class NonLiteralObject {
      @SuppressWarnings("unused")
      final NonLiteralObject next;

      public NonLiteralObject() {
        next = new NonLiteralObject(1);
      }

      private NonLiteralObject(int i) {
        if (i < 3) {
          next = new NonLiteralObject(++i);
        } else {
          next = null;
        }
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);
      String testClass = PartialCollectionsCloneTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      String methodExpr = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpr);
      config.addIncludePattern(testClass + "*");

      spec.addRoot("barrier", "barrier");
      spec.addRoot("hashmap", "hashmap");
      spec.addRoot("hashtable", "hashtable");
    }

  }

}
