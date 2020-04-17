/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
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
  protected static final String OVERRIDDEN    = "unrecognized thread id; thread state is unavailable";

  /**
   * Create some threads, and take a thread dump.
   */
  protected static String getDump(TraceThread[] threads) throws Exception {
    final Object lock = new Object();
    final String[] dump = new String[1];
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        // This lock should show up in the thread dump
        synchronized (lock) {
          dump[0] = ThreadDumpUtil.getThreadDump();
        }
      }
    };
    final int numThreads = threads.length;
    final ObserverGate gate = new ObserverGate(numThreads, runnable);

    for (int i = 0; i < numThreads; i++) {
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
  public static class TraceThread extends Thread {

    private ObserverGate gate;

    @Override
    public void run() {
      try {
        this.gate.waiter();
      } catch (final Exception e) {
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
  public static class BadIdThread extends TraceThread {

    @Override
    public long getId() {
      return super.getId() + 1000000L; // 1000000L to avoid collisions with system threads
    }

  }

  protected int countSubstrings(String src, String target) {
    int fromIndex = 0;
    int count = 0;
    while (fromIndex < src.length()) {
      final int index = src.indexOf(target, fromIndex);
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
   * Wait till N waiter threads are waiting, then run a Runnable on the master thread, and then release all the threads.
   */
  private static class ObserverGate {
    private final int      waiters;
    private final Object   lock = new Object();
    private final Runnable runnable;
    private int            waiting;
    private boolean        done = false;

    /**
     * @param waiters number of waiter threads, not including master thread
     */
    public ObserverGate(int waiters, Runnable runnable) {
      this.waiters = waiters;
      this.runnable = runnable;
    }

    public void master() throws InterruptedException {
      synchronized (this.lock) {
        while (this.waiting < this.waiters) {
          this.lock.wait();
        }
        this.runnable.run();
        this.done = true;
        this.lock.notifyAll();
      }
    }

    public void waiter() throws InterruptedException {
      synchronized (this.lock) {
        this.waiting++;
        this.lock.notifyAll();
        while (!this.done) {
          this.lock.wait();
        }
      }
    }
  }

}
