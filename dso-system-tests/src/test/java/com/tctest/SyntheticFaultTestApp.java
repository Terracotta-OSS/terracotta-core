/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

public class SyntheticFaultTestApp extends AbstractTransparentApp {

  private CyclicBarrier barrier;
  private MyIntfRoot root = new MyIntfRoot();

  public SyntheticFaultTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();
      
      if (index == 0) {
        MyIntf f = foo("test");
        root.setF(f);
        root.setS("Test String");
      }
      
      barrier.barrier();
      
      if (index == 1) {
        MyIntf f = root.getF();
        root.getS();
        f.f();
      }
      
      barrier.barrier();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = SyntheticFaultTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
    config.addIncludePattern("*"+testClass+"$*");
    //spec = config.getOrCreateSpec(MyIntf.class.getName());
    //spec = config.getOrCreateSpec(MyIntfRoot.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    new CyclicBarrierSpec().visit(visitor, config);

  }
  
  private static class MyIntfRoot {
    MyIntf f;
    String s;
    
    public MyIntfRoot() {
      super();
    }
    
    public synchronized void setF(MyIntf f) {
      this.f = f;
    }
    
    public synchronized MyIntf getF() {
      return f;
    }

    public synchronized String getS() {
      return s;
    }

    public synchronized void setS(String s) {
      this.s = s;
    }
  }

  MyIntf foo(final String f) {
    return new MyIntf() {
      public String f() {
        return f.toString();
      }
    };
  }

  interface MyIntf {
    String f();
  }

}
