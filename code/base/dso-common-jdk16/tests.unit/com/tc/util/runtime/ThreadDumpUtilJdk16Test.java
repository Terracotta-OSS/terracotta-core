/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.util.Assert;

public class ThreadDumpUtilJdk16Test extends ThreadDumpUtilTestBase {

  public void testThreadDump() throws Throwable {
    final int numThreads = 10;
    String dump = getDump(numThreads, TraceThread.class);
    
    try {
      assertTrue(dump.contains("- locked"));
      
      Assert.eval("The text \"Full thread dump \" should be present in the thread dump",
                  dump.indexOf("Full thread dump ") >= 0);
      
      // we expect to see all the created threads waiting on a CountDownLatch
      assertEquals(numThreads, countSubstrings(dump, OBSERVER_GATE));
    } catch (Throwable e) {
      System.err.println(dump);
      throw e;
    }
  }

  /**
   * Thread.getId() should be final but it isn't, so subclasses can break the contract.
   * When this happens we need to behave gracefully.  See CDV-1262.
   */
  public void testBadThreadId() throws Throwable {
    int numThreads = 10;
    String dump = getDump(numThreads, BadIdThread.class);
    
    try {
      Assert.eval("The text \"Full thread dump \" should be present in the thread dump",
                  dump.indexOf("Full thread dump ") >= 0);
      
      // we expect to see all the created threads waiting on a CountDownLatch
      assertEquals(numThreads, countSubstrings(dump, OBSERVER_GATE));
      
      // half the strings should be complaining about unrecognized IDs
      assertEquals(numThreads / 2, countSubstrings(dump, OVERRIDDEN));
    } catch (Throwable e) {
      System.err.println(dump);
      throw e;
    }
  }
}
