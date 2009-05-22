/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.test.TCTestCase;

import java.util.concurrent.CountDownLatch;

/**
 * Base class for ThreadDumpUtil tests
 */
public class ThreadDumpUtilTestBase extends TCTestCase {

  /** shows up in stack trace of a thread waiting on CountDownLatch.await() */
  protected static final String CDL_AWAIT = "java.util.concurrent.CountDownLatch.await";
  /** shows up in stack trace of a thread with an overridden thread ID */
  protected static final String OVERRIDDEN = "unrecognized thread id; thread state is unavailable";

  private static final Object lock = new Object();
  
  /**
   * Create some threads, and take a thread dump.
   */
  protected static String getDump(int numThreads, Class<? extends TraceThread> threadClazz) throws Exception {
    final CountDownLatch startLatch = new CountDownLatch(numThreads);
    final CountDownLatch doneLatch = new CountDownLatch(1);
    
    TraceThread[] threads = new TraceThread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] = threadClazz.newInstance();
      threads[i].init(startLatch, doneLatch);
      threads[i].start();
    }
    
    startLatch.await();
    String dump;
    synchronized(lock) {
      dump = ThreadDumpUtil.getThreadDump();
    }
    doneLatch.countDown();
    
    // Make sure all threads are gone before the next test starts
    for (int i = 0; i < numThreads; ++i) {
      threads[i].join();
    }
    
    return dump;
  }
  
  /**
   * A thread that we will look for on the stack trace
   */
  protected static class TraceThread extends Thread {
    
    private CountDownLatch startLatch;
    private CountDownLatch doneLatch;
    @Override
    public void run() {
      try {
        startLatch.countDown();
        doneLatch.await();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } 
    
    public void init(CountDownLatch start, CountDownLatch done) {
      this.startLatch = start;
      this.doneLatch = done;
    }
    
  }

  /**
   * A thread that overrides getId() - kids, don't try this at home
   */
  protected static class BadIdThread extends TraceThread {
    
    @Override
    public long getId() {
      long id = super.getId();
      // override half the ids
      id = (id % 2 == 0) ? id : id + 1000000L; // 1000000L to avoid collisions with system threads
      return id;
    }
    
  }

  protected int countSubstrings(String src, String target) {
    int fromIndex = 0;
    int count = 0;
    while (fromIndex < src.length()) {
      int index = src.indexOf(target, fromIndex);
      if (index >= 0) {
        count++;
        fromIndex = index + target.length();
      } else {
        return count;
      }
    }
    return 0;
  }

}
