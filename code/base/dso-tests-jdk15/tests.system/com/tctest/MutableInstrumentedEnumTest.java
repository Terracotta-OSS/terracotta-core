/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public class MutableInstrumentedEnumTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return MutableInstrumentedEnumTestApp.class;
  }

  public static class MutableInstrumentedEnumTestApp extends AbstractErrorCatchingTransparentApp {

    private final Map<String, Ref> mapRoot = new HashMap<String, Ref>();
    private final CyclicBarrier    barrier;

    public MutableInstrumentedEnumTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    protected void runTest() throws Throwable {

      int index = barrier.await();

      MutableEnum e = MutableEnum.THREE;

      if (index == 0) {
        synchronized (mapRoot) {
          mapRoot.put("e", new Ref(e));
        }
      }

      barrier.await();

      if (index == 0) {
        synchronized (mapRoot) {
          e.mutate();
        }
      }

      barrier.await();

      if (index != 0) {
        // acquiring this lock should force the potentially bad TXN (containing the change to the shared mutable enum)
        // to be flushed
        synchronized (mapRoot) {
          mapRoot.clear();
        }
      }

      barrier.await();

    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = MutableInstrumentedEnumTestApp.class.getName();
      ITransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.getOrCreateSpec(MutableEnum.class.getName());
      config.getOrCreateSpec(Ref.class.getName());

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("barrier", "barrier");
      spec.addRoot("mapRoot", "mapRoot");
    }

  }

  public enum MutableEnum {
    ONE, TWO, THREE;

    private int mutableState;

    int mutate() {
      return ++mutableState;
    }
  }

  private static class Ref {

    private final Object o;

    Ref(Object o) {
      this.o = o;
    }

    Object getO() {
      return o;
    }

  }

}
