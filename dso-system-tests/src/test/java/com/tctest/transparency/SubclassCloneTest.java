/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.transparency;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;



public class SubclassCloneTest extends TransparentTestBase {
  private static final int NODE_COUNT = 2;

  protected Class getApplicationClass() {
    return SubclassCloneTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  public static class SubclassCloneTestApp extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier;
    private Map                 root;

    public SubclassCloneTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    protected void runTest() throws Throwable {
      int index = barrier.barrier();

      if (index == 0) {
        root = new HashMap();
        synchronized (root) {
          root.put("object", new Subclass());
        }
      }

      barrier.barrier();

      if (index != 0) {
        Subclass sub;
        synchronized (root) {
          sub = (Subclass) root.get("object");
        }

        Subclass cloned = (Subclass) sub.clone();
        Assert.assertNotNull(cloned.getO());
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = SubclassCloneTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);
      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");

      new CyclicBarrierSpec().visit(visitor, config);

      config.addIncludePattern("*..*", false);
    }

  }

  private static class Base implements Cloneable {
    private final Object o = this;

    public Object getO() {
      return o;
    }
  }

  private static class Subclass extends Base implements Cloneable {

    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

  }

}
