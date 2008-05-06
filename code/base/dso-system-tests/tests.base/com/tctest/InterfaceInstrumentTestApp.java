/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;

public class InterfaceInstrumentTestApp extends AbstractErrorCatchingTransparentApp {
  private final CyclicBarrier barrier;
  private MyInterface root = new MyConcrete();

  public InterfaceInstrumentTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, com.tc.object.config.DSOClientConfigHelper config) {
    String testClass = InterfaceInstrumentTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
    new CyclicBarrierSpec().visit(visitor, config);

    String concreteClass = MyConcrete.class.getName();
    String interfaceName = MyInterface.class.getName();
    
    config.addIncludePattern(interfaceName + "+");
    
    // THIS IS INTENTIONALLY COMMENTED OUT TO MAKE SURE
    // THE TEST STILL PASSES. SEE CDV-144
    // config.addIncludePattern(concreteClass);

    config.addWriteAutolock("* " + concreteClass + "*.*(..)");
  }

  protected void runTest() throws Throwable {
    if (barrier.barrier() == 0) {
      root.add("one");
      root.add("two");
    }

    barrier.barrier();
    Assert.assertEquals(2, root.getSize());

  }
  
  static interface MyInterface {
    int getSize();
    void add(Object o);
  }
  
  static class MyConcrete implements MyInterface {
    protected List list = new ArrayList();
    
    public void add(Object o) {
      synchronized (list) {
        list.add(o);
      }
    }

    public int getSize() {
      synchronized (list) {
        return list.size();
      }
    }
  }
  
  
}
