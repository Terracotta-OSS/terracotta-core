/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;

public class BigStringHammerTest extends TransparentTestBase implements TestConfigurator {
  private static final int NODE_COUNT    = 2;
  private static final int THREADS_COUNT = 10;

  public BigStringHammerTest() {
    // disableAllUntil("2008-04-30");
  }

  protected Class getApplicationClass() {
    return BigStringHammerTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }

  public static class BigStringHammerTestApp extends AbstractErrorCatchingTransparentApp {

    private static final int ITEMS_PER_PARTICIPANT = 20;
    private static final int STRING_SIZE           = 1000;

    CyclicBarrier            barrier;
    List                     values;

    public BigStringHammerTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = BigStringHammerTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("values", "root");
      spec.addRoot("barrier", "barrier");
      String methodExpression = "* " + testClass + ".prepPhase(..)";
      config.addWriteAutolock(methodExpression);
      methodExpression = "* " + testClass + ".loadPhase(..)";
      config.addWriteAutolock(methodExpression);
      methodExpression = "* " + testClass + ".readPhase(..)";
      config.addReadAutolock(methodExpression);
      new CyclicBarrierSpec().visit(visitor, config);
    }

    public void runTest() throws BrokenBarrierException, InterruptedException {
      int participationCount = getParticipantCount();
      barrier = new CyclicBarrier(participationCount);

      int totalSize = participationCount * ITEMS_PER_PARTICIPANT;
      values = new ArrayList(totalSize);

      int arrivalOrder = barrier.barrier();

      prepPhase(arrivalOrder, totalSize);
      loadPhase(arrivalOrder);
      readPhase();
      
      System.out.println("Participant " + arrivalOrder + " done reading values.");
    }

    private void prepPhase(int arrivalOrder, int totalSize) {
      if (arrivalOrder == 0) {
        synchronized (values) {
          for (int i = 0; i < totalSize; i++) {
            values.add(null);
          }
        }
      }
    }

    private void loadPhase(int arrivalOrder) throws InterruptedException {
      barrier.barrier();

      // Set items in the list for this thread's chunk of the list
      int begin = arrivalOrder * ITEMS_PER_PARTICIPANT; // inclusive
      int end = (arrivalOrder + 1) * ITEMS_PER_PARTICIPANT; // exclusive

      System.out.println("Participant " + arrivalOrder + " preparing to add items " + begin + " to " + end);

      for (int i = begin; i < end; i++) {
        synchronized (values) {
          values.set(i, getBigString(i));
        }
      }
    }

    private String getBigString(int index) {
      StringBuffer str = new StringBuffer(STRING_SIZE);
      String part = "" + index;
      while (str.length() < STRING_SIZE) {
        str.append(part);
      }
      return str.toString();
    }

    private void readPhase() throws InterruptedException {
      for (int i = 0; i < values.size(); i++) {
        barrier.barrier();

        String actual = null;
        synchronized (values) {
          actual = (String) values.get(i);
        }
        String expected = getBigString(i);
        assertEquals(expected, actual);
      }
    }
  }
}
