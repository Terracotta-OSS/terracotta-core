/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.exception.TCClassNotFoundException;
import com.tc.object.bytecode.TCMap;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CyclicBarrier;

/**
 * (CDV-830) - This test confirms that a missing class on fault does not produce an IllegalMonitorStateException
 */
public class ConcurrentHashMapClassMissingOnFaultTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  private static class MyType {
    //
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier                 barrier;
    private final ConcurrentMap<String, Object> root = new ConcurrentHashMap<String, Object>();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    @Override
    protected void runTest() throws Throwable {
      int index = barrier.await();
      if (index == 0) {
        root.put("key", new MyType());
      }

      barrier.await();

      if (index != 0) {
        final IsolationClassLoader icl = (IsolationClassLoader) getClass().getClassLoader();
        icl.throwOnLoad("com.tctest.jdk15.ConcurrentHashMapClassMissingOnFaultTest$MyType", "XXX");

        assertEquals(0, ((TCMap) root).__tc_getAllLocalEntriesSnapshot().size());

        try {
          Object o = root.put("key", new Object());

          // using the return value of put() to make sure it will always be faulted
          throw new AssertionError(o.toString());
        } catch (Throwable t) {
          if ((t instanceof TCClassNotFoundException) && (t.getCause() instanceof ClassNotFoundException)
              && (t.getCause().getMessage().equals("XXX"))) {
            // expected
          } else {
            throw t;
          }
        }
      }

    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("barrier", "barrier");
      spec.addRoot("root", "root");
      config.addIncludePattern(MyType.class.getName());
    }
  }

}
