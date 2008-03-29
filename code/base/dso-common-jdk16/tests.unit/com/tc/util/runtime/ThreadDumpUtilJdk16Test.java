/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

import java.util.concurrent.CyclicBarrier;

import junit.framework.TestCase;

public class ThreadDumpUtilJdk16Test extends TestCase {

  public void testThreadDump() {
    CyclicBarrier barrier = new CyclicBarrier(10);
    
    for(int i = 0; i < 10; i++ ) {
      new WaitingThread(barrier).start();
    }
    
    //validate that correct thread dump is taken.
    String dump = ThreadDumpUtil.getThreadDump();
    assertTrue(dump.contains("Locked Monitors:") || dump.contains("Locked Synchronizers:"));
   
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
        
        //now wait.. for thread dump
        wait(5000);
      } catch (Exception ignore) {
        //ignore exception.. we just some threads to wait
        //to fill up the thread dump.
      } 
    }
    
    
  }
}
