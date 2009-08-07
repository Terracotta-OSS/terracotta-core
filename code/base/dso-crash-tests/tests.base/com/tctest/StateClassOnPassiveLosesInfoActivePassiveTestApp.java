/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public class StateClassOnPassiveLosesInfoActivePassiveTestApp extends AbstractTransparentApp {

  private static final int                ITERATION_COUNT = 50;

  private final CyclicBarrier             barrier;
  // For this test to work(fail when there is a problem), this root needs a map
  private final Map<Integer, StateObject> root            = new HashMap<Integer, StateObject>();

  public StateClassOnPassiveLosesInfoActivePassiveTestApp(String appId, ApplicationConfig cfg,
                                                          ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    Assert.assertEquals(2, getParticipantCount());
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();
      if (index == 0) {
        System.err.println("Entering stage 1");
        run1();
        System.err.println("Waiting for several server crashes to happen ... 90 secs ");
        ThreadUtil.reallySleep(90000);
        System.err.println("Entering stage 2");
        run2();
      }
      barrier.await();
      if (index != 0) {
        System.err.println("Entering stage 3 : verify stage");
        verify2();
      }

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void run1() {
    int count = 0;
    while (count < ITERATION_COUNT) {
      StateObject s = new StateObject();
      switch (count % 4) {
        case 0:
          s.ref = new Integer(77);
          break;
        case 1:
          s.ref = new Long(1977);
          break;
        case 2:
          s.ref = new Float(19.77);
          break;
        case 3:
          s.ref = new Double(1.977d);
          break;
      }
      synchronized (root) {
        root.put(new Integer(count++), s);
      }
      ThreadUtil.reallySleep(10);
    }
  }

  private void run2() {
    int count = 0;
    while (count < ITERATION_COUNT) {
      StateObject s = getStateObject(count++);
      synchronized (s) {
        switch (count % 4) {
          case 0:
            s.ref = new String("1977");
            break;
          case 1:
            s.ref = new Object();
            break;
          case 2:
            s.ref = new StateObject();
            break;
          case 3:
            s.ref = State.START;
            break;
        }
      }
      ThreadUtil.reallySleep(10);
    }
  }

  private void verify2() {
    int count = 0;
    while (count < ITERATION_COUNT) {
      StateObject s = getStateObject(count++);
      synchronized (s) {
        System.err.println(count + " - > " + s.ref);
        switch (count % 4) {
          case 0:
            Assert.assertEquals(s.ref, new String("1977"));
            break;
          case 1:
            Assert.assertTrue(s.ref != null && s.ref.getClass().getName().equals(Object.class.getName()));
            break;
          case 2:
            Assert.assertTrue(s.ref instanceof StateObject);
            break;
          case 3:
            Assert.assertEquals(State.START, s.ref);
            break;
        }
      }
      ThreadUtil.reallySleep(10);
    }
  }

  private StateObject getStateObject(int idx) {
    synchronized (root) {
      return root.get(new Integer(idx));
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = StateClassOnPassiveLosesInfoActivePassiveTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*", false, false, false);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
  }

  private static final class StateObject {

    Object ref;

    // XXX: Not sure if this variable is actually needed?
    @SuppressWarnings("unused")
    int    i;

  }

  private static enum State {
    START, END
  }

}
