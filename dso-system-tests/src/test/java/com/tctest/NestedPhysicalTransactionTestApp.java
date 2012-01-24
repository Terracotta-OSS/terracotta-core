/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.builtin.AtomicInteger;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.BrokenBarrierException;

public class NestedPhysicalTransactionTestApp extends AbstractTransparentApp {

  private final MyRoot        root;
  private final CyclicBarrier barrier;
  private final AtomicInteger participants;

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NestedPhysicalTransactionTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "the-data-root-yo");
    spec.addRoot("participants", testClass + ".participants");
    spec.addRoot("barrier", "barrier");
    config.addIncludePattern(MyRoot.class.getName());
  }

  public NestedPhysicalTransactionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    root = new MyRoot();
    barrier = new CyclicBarrier(this.getParticipantCount());
    participants = new AtomicInteger(0);
  }

  public void run() {
    int participantID = participants.incrementAndGet();
    try {
      int count = 0;
      for (int i = 0; i < this.getIntensity(); i++) {
        barrier.await();
        if (participantID == 1) {
          root.incrementByThree();
          barrier.await();
        } else {
          barrier.await();
          count += 3;
          int rootCount = root.getCount();
          if (rootCount != count) { throw new AssertionError("Expected " + count + " but was " + rootCount); }
        }
      }
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    } catch (BrokenBarrierException e) {
      throw new TCRuntimeException(e);
    }
  }

  private static final class MyRoot {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private int          count;

    public int getCount() {
      return count;
    }

    public void incrementByThree() {
      increment1();
    }

    private synchronized void increment1() {
      count++;
      increment2();
    }

    private void increment2() {
      synchronized (lock1) {
        count++;
        increment3();
      }
    }

    private void increment3() {
      synchronized (lock2) {
        count++;
      }
    }

  }

}
