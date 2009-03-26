/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueMultiThreadTestApp extends AbstractTransparentApp {
  private static final int    NUM_OF_PUTS    = 1000;
  private static final int    NUM_OF_THREADS = 5;

  private LinkedBlockingQueue queue          = new LinkedBlockingQueue(2);
  private final CyclicBarrier barrier;
  private int[]               putCount;
  private int[]               getCount;

  public LinkedBlockingQueueMultiThreadTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      final CyclicBarrier localBarrier = new CyclicBarrier(NUM_OF_THREADS + 1);
      if (index == 0) {
        Thread[] putter = new Thread[NUM_OF_THREADS];
        final int[] localPutCount = new int[NUM_OF_THREADS];
        for (int i = 0; i < NUM_OF_THREADS; i++) {
          final int k = i;
          localPutCount[k] = 0;
          putter[k] = new Thread(new Runnable() {
            public void run() {
              try {
                localBarrier.await();
                for (int j = 0; j < NUM_OF_PUTS; j++) {
                  int seed = (this.hashCode() ^ (int) System.nanoTime());
                  localPutCount[k] += seed;
                  queue.put(new Integer(seed));
                }
                localBarrier.await();
              } catch (Throwable t) {
                throw new AssertionError(t);
              }
            }

          });
        }
        for (int i = 0; i < NUM_OF_THREADS; i++) {
          putter[i].start();
        }
        localBarrier.await();
        localBarrier.await();
        this.putCount = localPutCount;
      } else {
        Thread[] getter = new Thread[NUM_OF_THREADS];
        final int[] localGetCount = new int[NUM_OF_THREADS];
        for (int i = 0; i < NUM_OF_THREADS; i++) {
          final int k = i;
          localGetCount[k] = 0;
          getter[k] = new Thread(new Runnable() {
            public void run() {
              try {
                localBarrier.await();
                for (int j = 0; j < NUM_OF_PUTS; j++) {
                  Integer o = (Integer) queue.take();
                  localGetCount[k] += o.intValue();
                }
                localBarrier.await();
              } catch (Throwable t) {
                throw new AssertionError(t);
              }

            }
          });
        }
        for (int i = 0; i < NUM_OF_THREADS; i++) {
          getter[i].start();
        }
        localBarrier.await();
        localBarrier.await();
        this.getCount = localGetCount;
      }
      barrier.await();

      if (index == 0) {
        int totalPutCount = 0;
        int totalGetCount = 0;
        for (int i = 0; i < NUM_OF_THREADS; i++) {
          totalPutCount += putCount[i];
          totalGetCount += getCount[i];
        }
        Assert.assertEquals(totalPutCount, totalGetCount);
      }

      barrier.await();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LinkedBlockingQueueMultiThreadTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("queue", "queue");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("putCount", "putCount");
    spec.addRoot("getCount", "getCount");
  }

}
