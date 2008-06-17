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
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Test case to make sure Hashtables Enumeration based views (ie. keys() and elements()) do not throw
 * ConcurrentModificationException. See DEV-1677, CDV-752
 */
public class HashtableEnumerationsTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private Hashtable           root;
    private final CyclicBarrier barrier;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());

    }

    protected void runTest() throws Throwable {
      // test an unshared Hashtable
      testEnumerations(newNonEmtpyHashtable());

      int index = barrier.barrier();
      if (index == 0) {
        root = newNonEmtpyHashtable();
      }

      barrier.barrier();

      synchronized (root) {
        // shared Hashtables should work too. The values should be lazily faulted in the 2nd node
        testEnumerations(root);
      }
    }

    private static Hashtable newNonEmtpyHashtable() {
      Hashtable rv = new Hashtable();
      addNonLiteralMapping(rv);
      addNonLiteralMapping(rv);
      return rv;
    }

    private static void addNonLiteralMapping(Hashtable ht) {
      ht.put("" + ht.size(), new ValueType());
    }

    private static void testEnumerations(Hashtable ht) {
      testEnumeration(ht, ht.elements(), ValueType.class);
      testEnumeration(ht, ht.keys(), String.class);
    }

    private static void testEnumeration(Hashtable ht, Enumeration e, Class expectedType) {
      addNonLiteralMapping(ht);

      // should NOT throw ConcurrentModificationException!
      while (e.hasMoreElements()) {
        Object o = e.nextElement();
        assertNotNull(o);

        // make sure it is the correct type -- as opposed to ObjectID or some TC wrapper type
        assertTrue(o.getClass().getName(), expectedType.isAssignableFrom(o.getClass()));
        System.err.println(o);
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);
      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.addIncludePattern(ValueType.class.getName());
      config.addIncludePattern(testClass + "$*");

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");
    }

    private static class ValueType {
      //
    }

  }

}
