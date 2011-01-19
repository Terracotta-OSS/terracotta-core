/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.runtime.cache.CacheMemoryEventType;
import com.tc.runtime.cache.CacheMemoryEventsListener;
import com.tc.runtime.cache.CacheMemoryManagerEventGenerator;
import com.tc.test.TCTestCase;

public class CacheMemoryEventGenaratorTest extends TCTestCase {

  private final Object belowThreshold      = new Object();
  private final Object aboveThreshold      = new Object();
  private boolean      belowThresholdRecvd = false;
  private boolean      aboveThresholdRecvd = false;

  private byte[]       temp;

  private void register() {
    TCThreadGroup thrdGrp = new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(CacheMemoryEventGenaratorTest.class)));
    TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(1L, 2, true, thrdGrp, true);
    int threshold = 20;
    int criticalThreshold = 30;
    int leastCount = 2;
    CacheMemoryEventsListener listener = new CacheMemoryEventsListenerImpl();
    new CacheMemoryManagerEventGenerator(threshold, criticalThreshold, leastCount, tcMemManager, listener);
  }

  public void test() throws Exception {
    register();
    temp = new byte[getByteArraySize()];
    // to remove warning
    temp[0] = 0;
    synchronized (belowThreshold) {
      if (!belowThresholdRecvd) belowThreshold.wait();
    }

    temp = new byte[getByteArraySize()];
    // to remove warning
    temp = null;
    System.gc();

    synchronized (aboveThreshold) {
      if (!aboveThresholdRecvd) aboveThreshold.wait();
    }
  }

  private class CacheMemoryEventsListenerImpl implements CacheMemoryEventsListener {
    public void memoryUsed(CacheMemoryEventType type, MemoryUsage usage) {
      System.err.println("Memeory Threshold=20 Memory usage=" + usage.getUsedPercentage() + " Event: " + type);
      if (type != CacheMemoryEventType.BELOW_THRESHOLD) {
        synchronized (belowThreshold) {
          belowThreshold.notify();
          belowThresholdRecvd = true;
        }
      } else {
        synchronized (aboveThreshold) {
          aboveThreshold.notify();
          aboveThresholdRecvd = true;
        }
      }
    }

  }

  private int getByteArraySize() {
    Runtime runtime = Runtime.getRuntime();
    long max_memory = runtime.maxMemory();
    if (max_memory == Long.MAX_VALUE) {
      // With no upperbound it is possible that this test wont pass
      throw new AssertionError("This test is memory sensitive. Please specify the max memory using -Xmx option. "
                               + "Currently Max Memory is " + max_memory);
    }
    System.err.println("Max memory is " + max_memory);
    int blockSize = (int) ((max_memory) / 4);
    System.err.println("Memory block size is " + blockSize);
    return blockSize;
  }
}
