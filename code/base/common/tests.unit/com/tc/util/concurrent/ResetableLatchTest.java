/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import junit.framework.TestCase;


/**
 * Test ResetableLatch
 */
public class ResetableLatchTest extends TestCase {

  public void testReset() {

    ResetableLatch latch = new ResetableLatch();

    // latch.reset();

    Context threadContext = new Context();

    //CASE 1: run two threads, that calls acquire on the latch
    Thread thread1 = new AcquireLatchThread(latch, threadContext);
    Thread thread2 = new AcquireLatchThread(latch, threadContext);

    thread1.start();
    thread2.start();

    if (threadContext.hasException()) { throw new AssertionError(threadContext.getException()); }

    // wait on acquire, should not breach yet.
    assertFalse(threadContext.hasBreachedAcquire());

    // release latch
    latch.release();

    // let the thread's join back
    try {
      thread1.join();
      thread2.join();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    // possibility of exception in thread
    if (threadContext.hasException()) { throw new AssertionError(threadContext.getException()); }

    // now acquire should be breached
    assertTrue(threadContext.hasBreachedAcquire());
    
    //CASE 2: let's try this again, and try the latch in broken and non-reset state
  
    //reset stuff.
    threadContext.clear();

    thread1 = new AcquireLatchThread(latch, threadContext);
    thread2 = new AcquireLatchThread(latch, threadContext);
    
    thread1.start();
    thread2.start();
    
    try {
      thread1.join();
      thread2.join();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    
    //should breach, since latch is already broken 
    assertTrue(threadContext.hasBreachedAcquire());
    
    
    
    //CASE 3: now let's try this again. with the latch reseted.
    
    
    //reset stuff.
    threadContext.clear();

    
    //now lets reset the latch
    latch.reset();
    
    
    
    
    thread1 = new AcquireLatchThread(latch, threadContext);
    thread2 = new AcquireLatchThread(latch, threadContext);

    thread1.start();
    thread2.start();

    if (threadContext.hasException()) { throw new AssertionError(threadContext.getException()); }

    // wait on acquire, should not breach yet.
    assertFalse(threadContext.hasBreachedAcquire());

    // release latch
    latch.release();

    // let the thread's join back
    try {
      thread1.join();
      thread2.join();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    // possibility of exception in thread
    if (threadContext.hasException()) { throw new AssertionError(threadContext.getException()); }

    // now acquire should be breached
    assertTrue(threadContext.hasBreachedAcquire());
 
  }

  private static final class AcquireLatchThread extends Thread {

    private final ResetableLatch latch;
    private final Context        context;

    public AcquireLatchThread(ResetableLatch latch, Context context) {
      this.latch = latch;
      this.context = context;
      setDaemon(true);
    }

    public void run() {

      try {
        latch.acquire();
      } catch (InterruptedException e) {
        context.setException(e);
      }
      context.breachedAcquire();
    }

  }

  private static final class Context {

    private Exception        exception       = null;

    private volatile Boolean breachedAcquire = Boolean.FALSE;

    public Exception getException() {
      return exception;
    }

    public void setException(Exception exception) {
      this.exception = exception;
    }

    public boolean hasException() {
      return exception != null ? true : false;
    }

    public void clear() {
      exception = null;
      breachedAcquire = Boolean.FALSE;
    }

    public void breachedAcquire() {
      breachedAcquire = Boolean.TRUE;
    }

    public boolean hasBreachedAcquire() {
      return breachedAcquire.booleanValue();
    }

  }

}
