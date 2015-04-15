/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.abortable;

import org.junit.Assert;

import java.util.concurrent.CyclicBarrier;
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
            barrier.await();
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
    barrier.await();
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
            barrier.await();
            try {
              Thread.currentThread().join();
            } catch (InterruptedException e) {
              Assert.assertTrue(abortableOperationManager.isAborted());
            }
            barrier.await();
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
    barrier.await();
    abortableOperationManager.abort(thread);
    try {
      abortableOperationManager.abort(thread);
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
      barrier.await();
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
  
/*  BS-959 - AbortableOperationManagerImpl class by itself is racey and can leak interrupts.  
  They way the class is used with NonStopManagerImpl keeps the leak from happening but really 
  thread safe use should be guaranteed within the class.  Comment out test for now until cleanup.
  
  public void testRace() {    
    final AtomicBoolean testFailed = new AtomicBoolean();
    final Thread first = new Thread() {
      public void run() {
        for (int x=0;x<Integer.MAX_VALUE;x++) {
          abortableOperationManager.begin();
          abortableOperationManager.finish();
          if (Thread.interrupted() ) {
            testFailed.set(true);
            break;
          }
        }
      }
    };
    Thread second = new Thread() {
      public void run() {
        for (int x=0;x<Integer.MAX_VALUE;x++) {
          try {
            abortableOperationManager.abort(first);
          } catch ( IllegalStateException ise ) {
            if ( !first.isAlive() ) {
              return;
            }
          }
        }
      }
    };
    
    first.start();
    second.start();
    try {
      first.join();
      second.join();
    } catch ( InterruptedException ie ) {
      throw new RuntimeException(ie);
    }
    if ( testFailed.get() ) {
      throw new AssertionError("thread was in interrupt state");
    }
  }
    */
}
