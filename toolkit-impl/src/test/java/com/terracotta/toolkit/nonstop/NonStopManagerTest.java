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
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManagerImpl;
import com.terracotta.toolkit.nonstop.NonStopManagerImpl.NonStopTaskWrapper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NonStopManagerTest extends TestCase {
  private AbortableOperationManagerImpl abortableOperationManager;
  private NonStopManagerImpl            nonStopManager;

  @Override
  protected void setUp() throws Exception {
    abortableOperationManager = new AbortableOperationManagerImpl();
    nonStopManager = new NonStopManagerImpl(abortableOperationManager);
  }

  public void testNonStopTimer() throws Exception {
    long startTime = System.nanoTime();
    int loopTmes = 4;
    long timeout = 500;
    for (int i = 0; i < loopTmes; i++) {
      System.out.println("executing loop count" + i);
      nonStopManager.begin(timeout);
      try {
        blockUntilAborted();
      } finally {
        Assert.assertTrue(abortableOperationManager.isAborted());
        Thread.currentThread().interrupt();
        nonStopManager.finish();
        // check that aborted status is cleared.
        Assert.assertFalse(abortableOperationManager.isAborted());
        // check that interrupted flag is cleared.
        Assert.assertFalse(Thread.interrupted());
      }
    }
    long timeTaken = System.nanoTime() - startTime;
    System.out.println("time taken to execute operations " + timeTaken);
    Assert
        .assertTrue((timeTaken >= loopTmes * TimeUnit.MILLISECONDS.toNanos(timeout) && timeTaken < (loopTmes
                                                                                                    * TimeUnit.MILLISECONDS
                                                                                                        .toNanos(timeout) + TimeUnit.SECONDS
            .toNanos(2))));
  }

  public void testTryBegin() throws Throwable {
    Assert.assertTrue(nonStopManager.tryBegin(10000));
    Assert.assertFalse(nonStopManager.tryBegin(10000));
    nonStopManager.finish();

    nonStopManager.begin(10000);
    Assert.assertFalse(nonStopManager.tryBegin(10000));
    nonStopManager.finish();

    Assert.assertTrue(nonStopManager.tryBegin(10000));
    try {
      nonStopManager.begin(-1);
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    } finally {
      nonStopManager.finish();
    }
  }

  public void testBoundaryTimeouts() {
    // test min value.
    nonStopManager.begin(Long.MIN_VALUE);
    nonStopManager.finish();
    // test negative value
    nonStopManager.begin(-1);
    nonStopManager.finish();
    // test zero
    nonStopManager.begin(0);
    nonStopManager.finish();
    // test max value
    nonStopManager.begin(Long.MAX_VALUE);
    nonStopManager.finish();
  }

  private void blockUntilAborted() {
    while (true) {
      try {
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        if (abortableOperationManager.isAborted()) { return; }
      }
    }
  }

  public void testIllegalStates() throws Throwable {
    // finish without begin
    try {
      nonStopManager.finish();
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    }
    // multiple begin
    nonStopManager.begin(10000);
    try {
      nonStopManager.begin(1000);
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    } finally {
      nonStopManager.finish();
    }

    // multiple finish
    nonStopManager.begin(10000);
    nonStopManager.finish();
    try {
      nonStopManager.finish();
      throw new AssertionError();
    } catch (IllegalStateException e) {
      // expected
    }
  }

  public void testNonStopManagerLeakTest() throws Throwable {
    List<WeakReference> weakReferences = new ArrayList<WeakReference>();
    for (int i = 0; i < 100; i++) {
      nonStopManager.begin(TimeUnit.MINUTES.toMillis(10));
      try {

        Collection<NonStopTaskWrapper> collection = nonStopManager.getTimerTasks().values();
        Assert.assertEquals(1, collection.size());

        NonStopTaskWrapper o = collection.iterator().next();
        weakReferences.add(new WeakReference(o.getFuture()));
      } finally {
        nonStopManager.finish();
      }
    }

    {
      Collection collection = nonStopManager.getTimerTasks().values();
      Assert.assertEquals(0, collection.size());
    }
    int gcRefCount = 0;
    for (long time = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2); System.currentTimeMillis() < time;) {
      gcRefCount = 0;

      System.err.println("Looping for GC");
      for (int i = 0; i < 10; i++) {
        System.gc();
      }
      for (WeakReference ref : weakReferences) {
        if (ref.get() == null) {
          gcRefCount++;
        }
      }
      if (gcRefCount > 95) {
        break;
      }

      Thread.sleep(500);
    }

    System.err.println("Test passed");

    Assert.assertTrue(gcRefCount > 95);
  }
}
