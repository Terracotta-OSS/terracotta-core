/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

/**
 * (see DEV-1808) -- This test reproduces a deadlock associated with clearing references on ConcurrentHashMap. The
 * relevant details of the test implementation are that one node makes updates to the CHM. In the other nodes size() is
 * being called and no mutations are made to the CHM. There needs to be a stream of mutations so that changes will be
 * broadcast to the read only nodes. The map is specifically not mutated in the read only nodes so that the CHM will be
 * candidate for clearing there.
 */
public class ConcurrentHashMapClearingDeadlockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private static final long   DURATION = 2 * 60 * 1000;
    private static final int    MAP_SIZE = 15000;

    private ConcurrentHashMap   mapRoot;
    private final CyclicBarrier barrier;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    private ConcurrentHashMap newMap(int size) {
      ConcurrentHashMap chm = new ConcurrentHashMap();
      for (int i = 0; i < size; i++) {
        chm.put(new Key(i), new Object());
      }
      return chm;
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.addIncludePattern(Key.class.getName());

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("mapRoot", "mapRoot");
      spec.addRoot("barrier", "barrier");

    }

    @Override
    protected void runTest() throws Throwable {
      int index = barrier.await();
      if (index == 0) {
        mapRoot = newMap(MAP_SIZE);
      }

      barrier.await();

      // fault the map
      Map root = mapRoot;

      barrier.await();

      Random r = new Random();
      final long end = System.currentTimeMillis() + DURATION;

      int count = 0;

      while (System.currentTimeMillis() < end) {
        count++;

        if (index != 0) {
          int size = root.size();

          if ((count % 50) == 0) {
            System.err.println("[" + System.currentTimeMillis() + "] [" + ManagerUtil.getClientID() + "]: " + size);
          }
        } else {
          root.put(new Key(r.nextInt(MAP_SIZE)), new Object());
        }
      }
    }
  }

  private static class Key {
    private final int value;

    Key(int value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Key) { return this.value == ((Key) obj).value; }
      return false;
    }

    @Override
    public int hashCode() {
      return value;
    }

  }

}
