/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;

import junit.framework.TestCase;

public class ThreadDumpUtilJdk16Test extends TestCase {

  public void testThreadDump() {
    CyclicBarrier barrier = new CyclicBarrier(10);

    for (int i = 0; i < 10; i++) {
      new WaitingThread(barrier).start();
    }

    synchronized (this) {
      // validate that correct thread dump is taken.
      String dump = ThreadDumpUtil.getThreadDump();
      assertTrue(dump.contains("- locked"));

      dump = ThreadDumpUtil.getThreadDump(new HashMap(), new HashMap(), new NullThreadIDMap());
      assertTrue(dump.contains("- locked"));
    }
  }

  private static class WaitingThread extends Thread {

    private CyclicBarrier barrier;

    public WaitingThread(CyclicBarrier barrier) {
      this.barrier = barrier;
    }

    @Override
    public void run() {
      try {
        barrier.await();

        // now wait.. for thread dump
        wait(5000);
      } catch (Exception ignore) {
        // ignore exception.. we just some threads to wait
        // to fill up the thread dump.
      }
    }

  }
}
