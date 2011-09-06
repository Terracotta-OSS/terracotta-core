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

public class NonStaticInnerTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier;
    private Top                 root;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    @Override
    protected void runTest() throws Throwable {

      final int index = barrier.barrier();

      if (index == 0) {
        root = new Top();
      }

      barrier.barrier();

      root.exerciseInner();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");

      config.addIncludePattern(Top.class.getName() + "*");
    }

    private static class Top {
      private final SubInner subInner = new SubInner();

      void exerciseInner() {
        subInner.subInnerMethod1();
        subInner.subInnerMethod2();
      }

      private void outerMethod() {
        System.err.println("hello");
      }

      class Inner {
        //

        void innerMethod() {
          outerMethod();
        }

      }

      class SubInner extends Inner {
        void subInnerMethod1() {
          innerMethod();
        }

        void subInnerMethod2() {
          outerMethod();
        }
      }
    }

  }

}
