/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.util.TCTimeoutException;

import junit.framework.TestCase;

/**
 * Test cases for TCFuture
 * 
 * @author teck
 */
public class TCFutureTest extends TestCase {

  public void testTimeout() {
    testTimeout(new TCFuture());

    Object lock = new Object();
    final TCFuture f1 = new TCFuture(lock);
    testTimeout(f1);
  }

  public void testTimeout(TCFuture f1) {

    try {
      f1.get(500);
      fail("timeout didn't happen");
    } catch (TCTimeoutException e) {
      // expected
    } catch (InterruptedException e) {
      fail(e.getMessage());
    } catch (TCExceptionResultException e) {
      fail(e.getMessage());
    }
  }

  public void testResultSet() {
    testResultSet(new TCFuture());
    Object lock = new Object();
    final TCFuture f1 = new TCFuture(lock);
    testResultSet(f1);
  }

  public void testResultSet(final TCFuture f1) {

    final Object val = new Object();

    Runnable run = new Runnable() {
      public void run() {
        try {
          Thread.sleep(1000);
        } catch (Throwable t) {
          // this should case the test case to fail
          f1.setException(new Throwable());
        }
        f1.set(val);
      }
    };

    new Thread(run).start();
    try {
      Object rv = f1.get();
      assertTrue(rv == val);
    } catch (InterruptedException e) {
      fail(e.getMessage());
    } catch (TCExceptionResultException e) {
      fail(e.getMessage());
    }
  }

  public void testSetMulti() {
    testSetMulti(new TCFuture());

    Object lock = new Object();
    final TCFuture f1 = new TCFuture(lock);
    testSetMulti(f1);
  }

  public void testSetMulti(final TCFuture f1) {

    f1.set(new Object());

    for (int i = 0; i < 50; i++) {
      try {
        f1.set(new Object());
        fail();
      } catch (IllegalStateException ise) {
        // expected
      }
    }
  }

  public void testSetAfterCancel() {
    testSetAfterCancel(new TCFuture());

    final TCFuture f1 = new TCFuture(new Object());
    testSetAfterCancel(f1);
  }

  public void testSetAfterCancel(final TCFuture f1) {

    f1.cancel();

    // this should only produce a warning log message
    f1.set(new Object());
  }

  public void testCancelAfterSet() {
    testCancelAfterSet(new TCFuture());

    final TCFuture f1 = new TCFuture(new Object());
    testCancelAfterSet(f1);
  }

  public void testCancelAfterSet(final TCFuture f1) {

    f1.set(new Object());

    // this should only produce a warning log message
    f1.cancel();
  }

  public void testCancel() {
    testCancel(new TCFuture());
    
    testCancel(new TCFuture(new Object()));
    
  }
  
  public void testCancel(final TCFuture f1) {
    Runnable run = new Runnable() {
      public void run() {
        f1.cancel();
      }
    };

    new Thread(run).start();
    try {
      f1.get(5000);
      fail("future wasn't cancelled");
    } catch (InterruptedException e) {
      // expected
    } catch (TCExceptionResultException e) {
      fail(e.getMessage());
    } catch (TCTimeoutException e) {
      fail(e.getMessage());
    }

    // should have no effect to cancel the future again
    f1.cancel();
  }

  public void testSetNull() throws Exception {
    testSetNull(new TCFuture());
    
    testSetNull(new TCFuture(new Object()));
  }
  
  public void testSetNull(final TCFuture f1) throws Exception {
    f1.set(null);

    assertTrue(f1.get(100) == null);
  }

  public void testSetNullException() {
    testSetNullException(new TCFuture());
    
    testSetNullException(new TCFuture(new Object()));
  }
  
  
  public void testSetNullException(final TCFuture f1) {
    try {
      f1.setException(null);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

  public void testExceptionAfterSet() {
    testExceptionAfterSet(new TCFuture());
    
    testExceptionAfterSet(new TCFuture(new Object()));
    
  }
  
  public void testExceptionAfterSet(final TCFuture f1) {
    f1.set(new Object());

    Throwable t = new Throwable("throw me");

    try {
      f1.setException(t);
      fail();
    } catch (IllegalStateException ise) {
      // expected
    }
  }

  public void testExceptionAfterCancel() {
    testExceptionAfterCancel(new TCFuture());
    testExceptionAfterCancel(new TCFuture(new Object()));
  }
  
  public void testExceptionAfterCancel(final TCFuture f1) {
  f1.cancel();

    Throwable t = new Throwable("throw me");

    // should just log a warning
    f1.setException(t);
  }

  public void testExceptionResult() {
    testExceptionResult(new TCFuture());
    testExceptionResult(new TCFuture(new Object()));
  }
  
  public void testExceptionResult(final TCFuture f1) {
    Throwable t = new Throwable("throw me");
    f1.setException(t);

    try {
      f1.get(1000);
      fail("exception not thrown");
    } catch (TCTimeoutException e) {
      fail(e.getMessage());
    } catch (InterruptedException e) {
      fail(e.getMessage());
    } catch (TCExceptionResultException e) {
      Throwable thrown = e.getCause();
      assertTrue(thrown == t);
    }
  }
}
