/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime;

import com.tc.util.Assert;
import com.tc.util.runtime.ThreadDumpUtilTestBase;

public class ThreadDumpUtilJdk15Test extends ThreadDumpUtilTestBase {

  public void testThreadDump15() throws Throwable {
    final int numThreads = 10;
    TraceThread[] threads = new TraceThread[numThreads];
    for (int i = 0; i < numThreads; ++i) {
      threads[i] = new TraceThread();
    }
    String dump = getDump(threads);
    
    try {
      Assert.eval("The text \"Full thread dump \" should be present in the thread dump",
                  dump.indexOf("Full thread dump ") >= 0);
      
      // we expect to see all the created threads waiting on a CountDownLatch
      assertEquals(numThreads, countSubstrings(dump, OBSERVER_GATE));
    } catch (Throwable t) {
      System.err.println(dump);
      throw t;
    }
  }

  /**
   * Thread.getId() should be final but it isn't, so subclasses can break the contract.
   * When this happens we need to behave gracefully.  See CDV-1262.
   */
  public void testBadThreadId() throws Throwable {
    final int numThreads = 10;
    TraceThread[] threads = new TraceThread[numThreads];
    for (int i = 0; i < numThreads; ++i) {
      threads[i] = (i % 2 == 0) ? new TraceThread() : new BadIdThread();
    }
    String dump = getDump(threads);
    
    try {
      Assert.eval("The text \"Full thread dump \" should be present in the thread dump",
                  dump.indexOf("Full thread dump ") >= 0);
      
      // we expect to see all the created threads waiting on a CountDownLatch
      assertEquals(numThreads, countSubstrings(dump, OBSERVER_GATE));
      
      // half the strings should be complaining about unrecognized IDs
      assertEquals(numThreads / 2, countSubstrings(dump, OVERRIDDEN));
    } catch (Throwable t) {
      System.err.println(dump);
      throw t;
    }
  }
  
}

