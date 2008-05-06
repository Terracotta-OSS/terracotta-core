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
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public class ShareInnerOfInnerClassTestApp extends AbstractTransparentApp {

  private OuterClass.InnerClass.InnieClass innieRoot = new OuterClass.InnerClass.InnieClass("mmkay");
  private CyclicBarrier                    barrier;
  private int[]                            count     = new int[1];

  private int                              myId;

  public ShareInnerOfInnerClassTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(cfg.getGlobalParticipantCount());
  }

  public void run() {
    synchronized (innieRoot) {
      Assert.assertEquals("mmkay", innieRoot.getString());
    }

    try {
      innieRoot.setString("I am smart. S.M.R.T.");
      throw new AssertionError("Supposed to throw UnlockedSharedObjectException error");
    } catch (UnlockedSharedObjectException e) {
      // expected
    }

    barrier();

    synchronized (count) {
      myId = count[0]++;
      
      if (myId == 0) {
        synchronized (innieRoot) {
          innieRoot.setString("D'oh");
        }
      }
      
    }

    barrier();
    
    if (myId == 1) {
      synchronized (innieRoot) {
        Assert.assertEquals("D'oh", innieRoot.getString());
      }
    }

  }

  private void barrier() {
    try {
      barrier.barrier();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ShareInnerOfInnerClassTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.getOrCreateSpec(OuterClass.InnerClass.InnieClass.class.getName());    

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("innieRoot", "innieRoot");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("count", "count");
    
    new CyclicBarrierSpec().visit(visitor, config);
  }

  private static class OuterClass {
    private static class InnerClass {
      private static class InnieClass {
        String myString;

        InnieClass(String s) {
          myString = s;
        }

        public void setString(String s) {
          myString = s;
        }

        public String getString() {
          return myString;
        }
      }
    }
  }
}
