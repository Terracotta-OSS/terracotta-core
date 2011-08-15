/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

/**
 * This to make sure objects whose hashCode() value happens to be equal to their System.identityHashCode() can be used
 * as keys in ConcurrentHashMaps (see CDV-615)
 */
public class ConcurrentHashMapIDHashCodeTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public ConcurrentHashMapIDHashCodeTest() {
    if (Vm.isIBM()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

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
    private static final int                     ID   = 69;

    private final ConcurrentHashMap<Key, String> root = new ConcurrentHashMap<Key, String>();
    private final CyclicBarrier                  barrier;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      testSpecialKeyForPut(index);

      barrier.await();

      testSpecialKeyForGet(index);
    }

    private void testSpecialKeyForPut(int index) throws Exception {
      if (index == 0) {
        root.clear();
        root.put(getSpecialKey(), "value");
      }

      barrier.await();

      Assert.assertEquals("value", root.get(getNonSpecialKey()));
    }

    private void testSpecialKeyForGet(int index) throws Exception {
      if (index == 0) {
        root.clear();
        Key key = getNonSpecialKey();
        root.put(key, "value");
      }

      barrier.await();

      if (index != 0) {
        Key key = getSpecialKey();
        Assert.assertEquals("value", root.get(key));
      }
    }

    private static Key getSpecialKey() {
      int count = 0;
      while (true) {
        count++;
        if ((count % 5000000) == 0) {
          System.err.println(count + " iterations so far");
        }
        Key key = new Key(ID);
        if (key.hashCode() == System.identityHashCode(key)) {
          System.err.println("Found instance after " + count + " iterations");
          return key;
        }
      }
    }

    private static Key getNonSpecialKey() {
      // It's not very likely this will ever take more than one loop iteration, but we want to make sure the hachCode()
      // and identityHashCode() are different
      while (true) {
        Key key = new Key(ID);
        if (key.hashCode() != System.identityHashCode(key)) { return key; }
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);
      config.addIncludePattern(Key.class.getName());

      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");
    }
  }

  private static class Key {

    private final int id;

    public Key(int id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Key)) return false;
      return id == ((Key) obj).id;
    }

    @Override
    public int hashCode() {
      return id;
    }
  }

}
