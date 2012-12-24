/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.abortable;

import org.junit.Assert;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class AbortableOperationManagerTest extends TestCase {
  private AbortableOperationManagerImpl abortableOperationManager;

  @Override
  protected void setUp() throws Exception {
    abortableOperationManager = new AbortableOperationManagerImpl();
  }

  public void testThreadAborted() throws Throwable {
    final CyclicBarrier barrier = new CyclicBarrier(2);
    final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          abortableOperationManager.begin();
          try {
            barrier.barrier();
            try {
              Thread.currentThread().join();
            } catch (InterruptedException e) {
              Assert.assertTrue(abortableOperationManager.isAborted());
            }
          } finally {
            abortableOperationManager.finish();
          }
        } catch (Throwable t) {
          exception.set(t);
        }
      }
    };
    thread.start();
    barrier.barrier();
    abortableOperationManager.abort(thread);
    thread.join();
    if (exception.get() != null) { throw new AssertionError(exception.get()); }
  }

  // TODO: should it be allowed
  public void testMultipleAbort() throws Throwable {
    final CyclicBarrier barrier = new CyclicBarrier(2);
    final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          abortableOperationManager.begin();
          try {
            barrier.barrier();
            try {
              Thread.currentThread().join();
            } catch (InterruptedException e) {
              Assert.assertTrue(abortableOperationManager.isAborted());
            }
            barrier.barrier();
          } finally {
            // set interrupted status this should get cleared on finish.
            Thread.currentThread().interrupt();
            abortableOperationManager.finish();
            Assert.assertFalse(Thread.interrupted());
          }
        } catch (Throwable t) {
          exception.set(t);
        }
      }
    };
    thread.start();
    barrier.barrier();
    abortableOperationManager.abort(thread);
    try {
      abortableOperationManager.abort(thread);
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
      barrier.barrier();
    }
    thread.join();
    if (exception.get() != null) { throw new AssertionError(exception.get()); }
  }

  public void testSelfAbortingThread() {
    abortableOperationManager.begin();
    try {
      abortableOperationManager.abort(Thread.currentThread());
      Assert.assertTrue(abortableOperationManager.isAborted());
      Assert.assertTrue(Thread.interrupted());
      Thread.currentThread().interrupt();
    } finally {
      abortableOperationManager.finish();
    }
    Assert.assertFalse(Thread.interrupted());
  }

  public void testInvalidStatesOperation() {
    // call finish without begin.
    try {
      abortableOperationManager.finish();
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    }

    // abort an unregistered thread.
    Thread t = new Thread();
    try {
      abortableOperationManager.abort(t);
    } catch (IllegalStateException e) {
      // expected
    }

    // call begin twice.
    abortableOperationManager.begin();
    try {
      abortableOperationManager.begin();
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected

    } finally {
      abortableOperationManager.finish();
    }

  }

}
