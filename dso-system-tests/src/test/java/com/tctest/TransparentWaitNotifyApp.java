/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;

public class TransparentWaitNotifyApp extends AbstractTransparentApp {
  private static final String DONE          = "done Biatch!";

  private static final int    INITIAL_STAGE = 0;
  private static final int    ABOUT_TO_WAIT = 1;
  private static final int    WAIT_COMPLETE = 2;

  private final HashMap       sharedRoot    = new HashMap();

  public TransparentWaitNotifyApp(String globalId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(globalId, cfg, listenerProvider);

    if ((getIntensity() != 1) || (getParticipantCount() != 2)) {
      //
      throw Assert.failure("Invalid parameters");
    }

  }

  public void run() {
    if ((new Integer(this.getApplicationId()).intValue() % 2) == 0) {
      testWaiter();
    } else {
      testNotify();
    }
    notifyResult(Boolean.TRUE);
  }

  private void testNotify() {
    moveToStage(INITIAL_STAGE);
    moveToStageAndWait(ABOUT_TO_WAIT);

    synchronized (sharedRoot) {
      System.err.println("NOTIFY");
      sharedRoot.notify();
    }

    moveToStageAndWait(WAIT_COMPLETE);

    synchronized (sharedRoot) {
      Assert.eval("key not in map", sharedRoot.containsKey(DONE));
    }
  }

  private void testWaiter() {
    moveToStage(INITIAL_STAGE);

    synchronized (sharedRoot) {
      moveToStage(ABOUT_TO_WAIT);

      System.err.println("WAIT");

      try {
        sharedRoot.wait();
        System.err.println("NOTIFIED");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }


      sharedRoot.put(DONE, null);
    }

    moveToStage(WAIT_COMPLETE);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = TransparentWaitNotifyApp.class.getName();
    config.addRoot(new Root(testClassName, "sharedRoot", "sharedRootLock"), true);
    String methodExpression = "* " + testClassName + ".*(..)";
    System.err.println("Adding autolock for: " + methodExpression);
    config.addWriteAutolock(methodExpression);
  }
}