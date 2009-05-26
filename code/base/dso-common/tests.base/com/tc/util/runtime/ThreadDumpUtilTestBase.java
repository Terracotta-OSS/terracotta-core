/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.test.TCTestCase;

/**
 * Base class for ThreadDumpUtil tests
 */
public class ThreadDumpUtilTestBase extends TCTestCase {

  /** shows up in stack trace of a thread waiting on ObserverGate.waiter() */
  protected static final String OBSERVER_GATE = "com.tc.util.runtime.ThreadDumpUtilTestBase$ObserverGate.waiter";
  /** shows up in stack trace of a thread with an overridden thread ID */
  protected static final String OVERRIDDEN = "unrecognized thread id; thread state is unavailable";

  /**
   * Create some threads, and take a thread dump.
   */
  protected static String getDump(int numThreads, Class<? extends TraceThread> threadClazz) throws Exception {
    final Object lock = new Object();
    final String[] dump = new String[1];
    Runnable runnable = new Runnable() {
      public void run() {
        // This lock should show up in the thread dump
        synchronized (lock) {
          dump[0] = ThreadDumpUtil.getThreadDump();
        }
      }
    };
    final ObserverGate gate = new ObserverGate(numThreads, runnable);
    
    TraceThread[] threads = new TraceThread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] = threadClazz.newInstance();
      threads[i].init(gate);
      threads[i].start();
    }
    
    gate.master();
    
    // Make sure all threads are gone before the next test starts
    for (int i = 0; i < numThreads; ++i) {
      threads[i].join();
    }
    
    return dump[0];
  }
  
  /**
   * A thread that we will look for on the stack trace
   */
  protected static class TraceThread extends Thread {
    
    private ObserverGate gate;
    @Override
    public void run() {
      try {
        gate.waiter();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } 
    
    public void init(ObserverGate g) {
      this.gate = g;
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
  
  /**
   * Wait till N waiter threads are waiting, then run a Runnable on the
   * master thread, and then release all the threads.
   */
  private static class ObserverGate {
    private final int waiters;
    private final Object lock = new Object();
    private final Runnable runnable;
    private int waiting;
    private boolean done = false;
    
    /**
     * @param waiters number of waiter threads, not including master thread
     */
    public ObserverGate(int waiters, Runnable runnable) {
      this.waiters = waiters;
      this.runnable = runnable;
    }
    
    public void master() throws InterruptedException {
      synchronized (lock) {
        while (waiting < waiters) {
          lock.wait();
        }
        runnable.run();
        done = true;
        lock.notifyAll();
      }
    }
    
    public void waiter() throws InterruptedException {
      synchronized (lock) {
        waiting++;
        lock.notifyAll();
        while (!done) {
          lock.wait();
        }
      }
    }
  }

}
