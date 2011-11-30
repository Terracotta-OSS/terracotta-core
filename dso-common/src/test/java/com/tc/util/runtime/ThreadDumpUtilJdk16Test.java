/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.util.Assert;

public class ThreadDumpUtilJdk16Test extends ThreadDumpUtilTestBase {

  public void testThreadDump() throws Throwable {
    final int numThreads = 10;
    TraceThread[] threads = new TraceThread[numThreads];
    for (int i = 0; i < numThreads; ++i) {
      threads[i] = new TraceThread();
    }
    String dump = getDump(threads);
    
    try {
      assertTrue(dump.contains("- locked"));
      
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
   * We used to use Thread.getId in ThreadDumpUtilJdk16.getThreadDump(). Since Thread.getId
   * method is not final so if any thread override the method we used to get Exception
   * DEV-3897 changed the behavior for ThreadDumpUtilJdk16.getThreadDump and so we get the 
   * correct thread dump even in case when the Thread.getId() method id overridden. 
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
      assertEquals(0, countSubstrings(dump, OVERRIDDEN));
    } catch (Throwable t) {
      System.err.println(dump);
      throw t;
    }
  }
}
