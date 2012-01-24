/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.builtin.HashMap;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Map;

public class TransparentWaitNotifyApp extends AbstractTransparentApp {
  private static final String DONE       = "done Biatch!";

  private final Map           sharedRoot = new HashMap();
  private final CyclicBarrier barrier    = new CyclicBarrier(getParticipantCount());

  public TransparentWaitNotifyApp(String globalId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(globalId, cfg, listenerProvider);

    if ((getIntensity() != 1) || (getParticipantCount() != 2)) {
      //
      throw Assert.failure("Invalid parameters");
    }

  }

  public void run() {
    try {
      if ((new Integer(this.getApplicationId()).intValue() % 2) == 0) {
        testWaiter();
      } else {
        testNotify();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    notifyResult(Boolean.TRUE);
  }

  private void testNotify() throws Exception {
    barrier.await();

    synchronized (sharedRoot) {
      System.err.println("NOTIFY");
      sharedRoot.notify();
    }

    barrier.await();

    synchronized (sharedRoot) {
      Assert.eval("key not in map", sharedRoot.containsKey(DONE));
    }
  }

  private void testWaiter() throws Exception {

    synchronized (sharedRoot) {
      barrier.await();

      System.err.println("WAIT");

      try {
        sharedRoot.wait();
        System.err.println("NOTIFIED");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      sharedRoot.put(DONE, null);
    }

    barrier.await();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = TransparentWaitNotifyApp.class.getName();
    config.addRoot(new Root(testClassName, "sharedRoot", "sharedRootLock"), true);
    String methodExpression = "* " + testClassName + ".*(..)";
    System.err.println("Adding autolock for: " + methodExpression);
    config.addWriteAutolock(methodExpression);

    TransparencyClassSpec spec = config.getOrCreateSpec(TransparentWaitNotifyApp.class.getName());
    spec.addRoot("barrier", "barrier");
  }
}
