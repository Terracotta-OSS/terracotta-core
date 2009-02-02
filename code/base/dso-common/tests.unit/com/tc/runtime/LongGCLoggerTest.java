/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.LogLevel;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LongGCLoggerTest extends TCTestCase {

  private CountDownLatch latch        = new CountDownLatch(1);
  private final int      LOOP_COUNT   = 20;
  private final int      OBJECT_COUNT = 1000;

  public void test() throws Exception {
    register();
    // Create some data for GC in a diff thread
    createThreadAndCollectGarbage();
    // wait in a thread to get notified
    latch.await(); 
  }

  private void createThreadAndCollectGarbage() {
    Runnable runnable = new Runnable() {
      public void run() {
        createGarbage();
      }
    };
    new Thread(runnable).start();
  }

  private void createGarbage() {
    byte[][] byteArray = null;
    byteArray = new byte[10][];

    for (int i = 0; i < LOOP_COUNT; i++) {
      if (i != 0) System.gc();

      for (int j = 0; j < 10; j++) {
        int length = getByteArraySize() / 10;
        byteArray[j] = new byte[length];
      }

      addObjectsToArrayList();
    }
  }

  private void addObjectsToArrayList() {
    List<Integer> list = new ArrayList<Integer>();

    for (int i = 0; i < OBJECT_COUNT; i++) {
      list.add(new Integer(i));
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

  private void register() {
    TCLogger tcLogger = new TCLoggerImpl();
    TCThreadGroup thrdGrp = new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(LongGCLoggerTest.class)));
    TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(1L, 2, true, thrdGrp);
    LongGCLogger logger = new LongGCLogger(tcLogger, 1);
    tcMemManager.registerForMemoryEvents(logger);
  }

  class TCLoggerImpl implements TCLogger {
    public void debug(Object message) {
      //
    }

    public void debug(Object message, Throwable t) {
      //
    }

    public void error(Object message) {
      //
    }

    public void error(Object message, Throwable t) {
      //
    }

    public void fatal(Object message) {
      //
    }

    public void fatal(Object message, Throwable t) {
      //
    }

    public LogLevel getLevel() {
      return null;
    }

    public String getName() {
      return null;
    }

    public void info(Object message) {
      //
    }

    public void info(Object message, Throwable t) {
      //
    }

    public boolean isDebugEnabled() {
      return false;
    }

    public boolean isInfoEnabled() {
      return false;
    }

    public void setLevel(LogLevel level) {
      //
    }

    public void warn(Object message) {
      System.err.println(message);

      latch.countDown();
    }

    public void warn(Object message, Throwable t) {
      //
    }

  }

}
