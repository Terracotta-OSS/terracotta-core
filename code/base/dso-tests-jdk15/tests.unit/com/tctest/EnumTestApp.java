/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;

public class EnumTestApp extends AbstractTransparentApp {

  private final DataRoot      dataRoot = new DataRoot();
  private final CyclicBarrier barrier;

  public EnumTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      if (index == 0) {
        dataRoot.setState(State.START);
      }

      barrier.await();

      Assert.assertTrue(dataRoot.getState() == State.START);
      Assert.assertEquals(0, dataRoot.getState().getStateNum());

      barrier.await();

      if (index == 1) {
        dataRoot.setState(State.RUN);
        dataRoot.getState().setStateNum(10);
      }

      barrier.await();

      Assert.assertTrue(dataRoot.getState() == State.RUN);
      if (index == 1) {
        Assert.assertEquals(10, dataRoot.getState().getStateNum());
      } else {
        // Mutation in enum is not supported.
        Assert.assertEquals(1, dataRoot.getState().getStateNum());
      }

      barrier.await();

      if (index == 0) {
        dataRoot.setState(State.STOP);
      }

      barrier.await();

      Assert.assertTrue(dataRoot.getState() == State.STOP);
      Assert.assertEquals(2, dataRoot.getState().getStateNum());

      barrier.await();

      if (index == 0) {
        dataRoot.setStates(State.values().clone());
      }

      barrier.await();

      Assert.assertEquals(3, dataRoot.getStates().length);
      Assert.assertTrue(Arrays.equals(State.values(), dataRoot.getStates()));

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = EnumTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$DataRoot*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClass + "$DataRoot*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("dataRoot", "dataRoot");
  }

  public enum State {
    START(0), RUN(1), STOP(2);

    private int stateNum;

    State(int stateNum) {
      this.stateNum = stateNum;
    }

    int getStateNum() {
      return this.stateNum;
    }

    void setStateNum(int stateNum) {
      this.stateNum = stateNum;
    }
  }

  private static class DataRoot {
    private State state;
    private State states[];

    public DataRoot() {
      super();
    }

    public synchronized void setState(State state) {
      this.state = state;
    }

    public synchronized State getState() {
      return state;
    }

    public synchronized State[] getStates() {
      return states;
    }

    public synchronized void setStates(State[] states) {
      this.states = states;
    }

  }
}
