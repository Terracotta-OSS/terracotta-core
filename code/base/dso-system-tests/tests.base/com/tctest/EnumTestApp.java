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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public class EnumTestApp extends AbstractTransparentApp {

  private final DataRoot                   dataRoot = new DataRoot();
  private final CyclicBarrier              barrier;
  private final Map<String, Object>        raceRoot = new HashMap<String, Object>();
  private final Map<String, ClassWithEnum> shareMap = new HashMap<String, ClassWithEnum>();

  private State                            stateRoot;

  public EnumTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      shareSubEnumTest(index);

      barrier.await();

      rootEnumTest(index);

      if (index == 0) {
        stateRoot = State.START;
      }

      barrier.await();

      Assert.assertTrue(stateRoot == State.START);

      barrier.await();

      if (index == 1) {
        stateRoot = State.RUN;
      }

      barrier.await();

      Assert.assertTrue(stateRoot == State.RUN);

      barrier.await();

      for (int i = 0; i < 100; i++) {
        if (index == 0) {
          stateRoot = State.START;
        } else {
          stateRoot = State.RUN;
        }
      }

      barrier.await();

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

      if (index == 0) {
        testRace();
      }

      if (index == 0) {
        testRace();
      }

      System.err.println("Entering final stage : Just to make the test run longer.");
      for (int i = 0; i < 5000; i++) {
        switch(i % 3) {
          case 0 :
            dataRoot.setState(State.RUN);
            break;
          case 1 :
            dataRoot.setState(State.START);
            break;
          case 2 :
            dataRoot.setState(State.STOP);
        }
        ThreadUtil.reallySleep(10);
        if(i % 500 == 0) {
          System.err.println(Thread.currentThread().getName() + " : Completed : " + i);
        }
      }

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void shareSubEnumTest(int index) throws Exception {
    if (index == 0) {
      synchronized (shareMap) {
        shareMap.put("classWithEnum", new ClassWithEnum());
      }
    }

    barrier.await();

    synchronized (shareMap) {
      ClassWithEnum o = shareMap.get("classWithEnum");
      Assert.assertEquals(EnumWithSubState.RUN, o.state);
    }

    barrier.await();
  }

  // Don't reference this enum in any other methods except testRace() please
  enum EnumForRace {
    V1, V2, V3;
  }

  private static class Ref {
    private final EnumForRace e;

    Ref(EnumForRace e) {
      this.e = e;
    }

    public String toString() {
      return "ref(" + e + ")";
    }
  }

  private void testRace() throws InterruptedException {
    final Object lock1 = new Object();
    final Object lock2 = new Object();

    synchronized (raceRoot) {
      raceRoot.put("lock1", lock1);
      raceRoot.put("lock1", lock2);
    }

    final CyclicBarrier cb = new CyclicBarrier(2);
    final EnumForRace enumInstance = EnumForRace.V1;

    Thread other = new Thread() {
      public void run() {
        shareWithLock(lock1, new Ref(enumInstance), raceRoot, cb);
      }
    };
    other.start();

    shareWithLock(lock2, new Ref(enumInstance), raceRoot, cb);

    other.join();
  }

  private static void shareWithLock(Object lock, Object toShare, Map<String, Object> root, CyclicBarrier barrier) {
    synchronized (lock) {
      root.put(String.valueOf(System.identityHashCode(lock)), toShare);

      try {
        barrier.await();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void rootEnumTest(int index) throws Exception {
    if (index == 0) {
      stateRoot = State.START;
    }

    barrier.await();

    Assert.assertEquals(State.START, stateRoot);

    barrier.await();

    if (index == 1) {
      stateRoot = State.RUN;
    }

    barrier.await();

    Assert.assertEquals(State.RUN, stateRoot);

    barrier.await();
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
    spec.addRoot("stateRoot", "stateRoot", false);
    spec.addRoot("raceRoot", "raceRoot");
    spec.addRoot("shareMap", "shareMap");

    // explicitly including the enum class here exposes a bug,
    // generally an enum type doesn't need to be included to be shared
    config.addIncludePattern(EnumForRace.class.getName());
    config.addIncludePattern(Ref.class.getName());
    config.addIncludePattern(ClassWithEnum.class.getName());
    config.addIncludePattern(EnumWithSubState.class.getName() + "$*");
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

  public enum EnumWithSubState {
    START {
      void setStateNum(int stateNum) {
        stateNum = 101;
      }
    },
    RUN {
      void setStateNum(int stateNum) {
        stateNum = 102;
      }
    },
    STOP {
      void setStateNum(int stateNum) {
        stateNum = 103;
      }
    };

    private int stateNum;

    int getStateNum() {
      return this.stateNum;
    }

    abstract void setStateNum(int stateNum);
  }

  private static class ClassWithEnum {
    private EnumWithSubState state;

    public ClassWithEnum() {
      state = EnumWithSubState.valueOf("RUN");
    }
  }
}
