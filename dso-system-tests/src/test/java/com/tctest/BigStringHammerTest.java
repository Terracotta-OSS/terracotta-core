/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

/**
 * The goal of this test is to check concurrent decompression of big strings.  "Big" (default=512) strings
 * get decompressed in an L1 before being sent to the L2.  These compressed values are then sent to any other
 * receiving L1s that fault in the String value.
 * 
 * On the receiving L1, we pack the compressed byte[] into a char[] and create a new String with it.  This string
 * has a valid length and hash code (retrieving only those values should not cause decompression).  But the string
 * is instrumented such that any call to the internal char[] goes through a new method that will unpack and decompress 
 * the char[] value before returning it.  
 * 
 * The act of decompression has a benign data race (similar to the one in computing the hashcode).  Since no
 * synchronization or volatile is used, more than one thread might decompress the string at the same time without 
 * knowing about the other thread's activity.  
 * 
 * This test tries to test decompression and also push that race a bit.  It starts multiple nodes with 
 * multiple threads.  Each thread grabs a portion of a shared list and populates it with a value based on the index
 * (so the expected value can be recomputed).
 * 
 * Then all threads walk through each index and:
 *   1) hit a barrier to synchronize
 *   2) access the string such that decompression will occur
 *   3) verify the string value against a newly constructed equivalent version
 *   
 * At any given index, 1 node will have created the values and so will not have a compressed value.  The interesting
 * part here is really in the threads, so you could bump up the THREAD_COUNT if you wanted.
 */
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
