/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.TCMap;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

/**
 * This test makes sure (at least for logical operations) that order in which objects are changed is preserved when
 * applied in another node (DEv-3550)
 */
public class ObjectChangeOrderTest extends TransparentTestBase {

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(5);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final Object                  lock    = new Object();
    private Map<String, Object>           map1;
    private CustomHashMap<String, Object> map2;
    private final CyclicBarrier           barrier = new CyclicBarrier(getParticipantCount());

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      int index = barrier.await();

      // this bizarre logic is to get the ObjectIDs to not be sequential
      if (index != 0) {
        map2 = new CustomHashMap<String, Object>();
      } else {
        map1 = new HashMap<String, Object>();
      }

      barrier.await();

      if (index != 0) {
        map2.setDependantMap(map1);
      }

      barrier.await();

      if (index == 0) {
        for (int i = 0; i < 100; i++) {
          synchronized (lock) {
            // put()s are always ordered (map1 first, then map2)
            put(map1);
            put(map2);
          }
        }
      }

      barrier.await();
    }

    private void put(Map<String, Object> map) {
      Object prev = map.put(String.valueOf(map.size()), new Object());
      if (prev != null) { throw new AssertionError("prev: " + prev); }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("barrier", "barrier");
      spec.addRoot("map1", "map1");
      spec.addRoot("map2", "map2");
      spec.addRoot("lock", "lock");

      config.addWriteAutolock("* " + App.class.getName() + ".*(..)");

      spec = config.getOrCreateSpec(CustomHashMap.class.getName());
      spec.setHonorTransient(true);
    }

    private static class CustomHashMap<K, V> extends HashMap<K, V> implements TCMap {

      private volatile transient Map<String, Object> map1;

      CustomHashMap() {
        //
      }

      void setDependantMap(Map<String, Object> map1) {
        this.map1 = map1;
      }

      public void __tc_applicator_clear() {
        throw new AssertionError();
      }

      public void __tc_applicator_put(Object key, Object value) {
        if (!map1.containsKey(key)) {
          //
          throw new AssertionError("missing data in map1: " + key);
        }
      }

      public void __tc_applicator_remove(Object key) {
        throw new AssertionError();
      }

      public Collection __tc_getAllEntriesSnapshot() {
        throw new AssertionError();
      }

      public Collection __tc_getAllLocalEntriesSnapshot() {
        throw new AssertionError();
      }

      public void __tc_put_logical(Object key, Object value) {
        throw new AssertionError();
      }

      public void __tc_remove_logical(Object key) {
        throw new AssertionError();
      }

    }

  }

}
