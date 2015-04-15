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
package com.terracotta.toolkit.collections;

import org.apache.commons.lang.ArrayUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.AssertionFailedError;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Contains convinient helpers to test concurrent collections. Based on the original JSR166 unit tests.
 *
 * @author Eugene Shelestovich
 */
public abstract class BaseConcurrentCollectionsTest {

  protected static final long SHORT_DELAY_MS = 50L;
  protected static final long SMALL_DELAY_MS = SHORT_DELAY_MS * 5;
  protected static final long MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
  protected static final long LONG_DELAY_MS = SHORT_DELAY_MS * 200;

  protected static final int TIMEOUT_MS = 10;

  /**
   * The number of elements to place in collections, arrays, etc.
   */
  protected static final int SIZE = 20;


  /**
   * The first exception encountered if any threadAssertXXX method fails.
   */
  private final AtomicReference<Throwable> threadFailure
      = new AtomicReference<Throwable>(null);

  /**
   * Records an exception so that it can be rethrown later in the test
   * harness thread, triggering a test case failure.  Only the first
   * failure is recorded; subsequent calls to this method from within
   * the same test have no effect.
   */
  public void threadRecordFailure(Throwable t) {
    threadFailure.compareAndSet(null, t);
  }

  /**
   * Extra checks that get done for all test cases.
   * <p/>
   * Triggers test case failure if any thread assertions have failed,
   * by rethrowing, in the test harness thread, any exception recorded
   * earlier by threadRecordFailure.
   * <p/>
   * Triggers test case failure if interrupt status is set in the main thread.
   */
  public void tearDown() throws Exception {
    Throwable t = threadFailure.getAndSet(null);
    if (t != null) {
      if (t instanceof Error)
        throw (Error)t;
      else if (t instanceof RuntimeException)
        throw (RuntimeException)t;
      else if (t instanceof Exception)
        throw (Exception)t;
      else {
        AssertionFailedError afe =
            new AssertionFailedError(t.toString());
        afe.initCause(t);
        throw afe;
      }
    }

    if (Thread.interrupted())
      throw new AssertionFailedError("interrupt status set in main thread");
  }

  /**
   * Just like assertTrue(b), but additionally recording (using
   * threadRecordFailure) any AssertionFailedError thrown, so that
   * the current testcase will fail.
   */
  public void threadAssertTrue(boolean b) {
    try {
      assertTrue(b);
    } catch (AssertionFailedError t) {
      threadRecordFailure(t);
      throw t;
    }
  }

  /**
   * Records the given exception using {@link #threadRecordFailure},
   * then rethrows the exception, wrapping it in an
   * AssertionFailedError if necessary.
   */
  public void threadUnexpectedException(Throwable t) {
    threadRecordFailure(t);
    t.printStackTrace();
    if (t instanceof RuntimeException)
      throw (RuntimeException)t;
    else if (t instanceof Error)
      throw (Error)t;
    else {
      AssertionFailedError afe =
          new AssertionFailedError("unexpected exception: " + t);
      afe.initCause(t);
      throw afe;
    }
  }

  /**
   * Delays, via Thread.sleep, for the given millisecond delay, but
   * if the sleep is shorter than specified, may re-sleep or yield
   * until time elapses.
   */
  static void delay(long millis) throws InterruptedException {
    long startTime = System.nanoTime();
    long ns = millis * 1000 * 1000;
    for (; ; ) {
      if (millis > 0L)
        Thread.sleep(millis);
      else // too short to sleep
        Thread.yield();
      long d = ns - (System.nanoTime() - startTime);
      if (d > 0L)
        millis = d / (1000 * 1000);
      else
        break;
    }
  }

  /**
   * Waits out termination of a thread pool or fails doing so.
   */
  void joinPool(ExecutorService exec) {
    try {
      exec.shutdown();
      assertTrue("ExecutorService did not terminate in a timely manner",
          exec.awaitTermination(2 * LONG_DELAY_MS, MILLISECONDS));
    } catch (SecurityException ok) {
      // Allowed in case test doesn't have privs
    } catch (InterruptedException ie) {
      fail("Unexpected InterruptedException");
    }
  }

  /**
   * Checks that thread does not terminate within the default
   * millisecond delay of {@code timeoutMillis()}.
   */
  void assertThreadStaysAlive(Thread thread) {
    assertThreadStaysAlive(thread, TIMEOUT_MS);
  }

  /**
   * Checks that thread does not terminate within the given millisecond delay.
   */
  void assertThreadStaysAlive(Thread thread, long millis) {
    try {
      // No need to optimize the failing case via Thread.join.
      delay(millis);
      assertTrue(thread.isAlive());
    } catch (InterruptedException ie) {
      fail("Unexpected InterruptedException");
    }
  }

  /**
   * Spin-waits up to the specified number of milliseconds for the given
   * thread to enter a wait state: BLOCKED, WAITING, or TIMED_WAITING.
   */
  void waitForThreadToEnterWaitState(Thread thread, long timeoutMillis) {
    long startTime = System.nanoTime();
    for (; ; ) {
      Thread.State s = thread.getState();
      if (s == Thread.State.BLOCKED ||
          s == Thread.State.WAITING ||
          s == Thread.State.TIMED_WAITING)
        return;
      else if (s == Thread.State.TERMINATED)
        fail("Unexpected thread termination");
      else if (millisElapsedSince(startTime) > timeoutMillis) {
        threadAssertTrue(thread.isAlive());
        return;
      }
      Thread.yield();
    }
  }

  /**
   * Returns the number of milliseconds since time given by
   * startNanoTime, which must have been previously returned from a
   * call to {@link System#nanoTime()}.
   */
  long millisElapsedSince(long startNanoTime) {
    return NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
  }

  /**
   * Returns a new started daemon Thread running the given runnable.
   */
  Thread newStartedThread(Runnable runnable) {
    Thread t = new Thread(runnable);
    t.setDaemon(true);
    t.start();
    return t;
  }

  /**
   * Waits for the specified time (in milliseconds) for the thread
   * to terminate (using {@link Thread#join(long)}), else interrupts
   * the thread (in the hope that it may terminate later) and fails.
   */
  void awaitTermination(Thread t, long timeoutMillis) {
    try {
      t.join(timeoutMillis);
    } catch (InterruptedException ie) {
      threadUnexpectedException(ie);
    } finally {
      if (t.getState() != Thread.State.TERMINATED) {
        t.interrupt();
        fail("Test timed out");
      }
    }
  }

  /**
   * Waits for LONG_DELAY_MS milliseconds for the thread to
   * terminate (using {@link Thread#join(long)}), else interrupts
   * the thread (in the hope that it may terminate later) and fails.
   */
  void awaitTermination(Thread t) {
    awaitTermination(t, LONG_DELAY_MS);
  }

  // Some convenient Runnable classes

  public abstract class CheckedRunnable implements Runnable {
    protected abstract void realRun() throws Throwable;

    public final void run() {
      try {
        realRun();
      } catch (Throwable t) {
        threadUnexpectedException(t);
      }
    }
  }

  public void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(LONG_DELAY_MS, MILLISECONDS));
    } catch (Throwable t) {
      threadUnexpectedException(t);
    }
  }

  public void await(Semaphore semaphore) {
    try {
      assertTrue(semaphore.tryAcquire(LONG_DELAY_MS, MILLISECONDS));
    } catch (Throwable t) {
      threadUnexpectedException(t);
    }
  }

  /**
   * A CyclicBarrier that uses timed await and fails with
   * AssertionFailedErrors instead of throwing checked exceptions.
   */
  public static class CheckedBarrier extends CyclicBarrier {
    public CheckedBarrier(int parties) { super(parties); }

    public int await() {
      try {
        return super.await(2 * LONG_DELAY_MS, MILLISECONDS);
      } catch (TimeoutException e) {
        throw new AssertionFailedError("timed out");
      } catch (Exception e) {
        AssertionFailedError afe =
            new AssertionFailedError("Unexpected exception: " + e);
        afe.initCause(e);
        throw afe;
      }
    }
  }

  void checkEmpty(BlockingQueue q) {
    try {
      assertTrue(q.isEmpty());
      assertEquals(0, q.size());
      assertNull(q.peek());
      assertNull(q.poll());
      assertNull(q.poll(0, MILLISECONDS));
      assertEquals(q.toString(), "[]");
      assertTrue(Arrays.equals(q.toArray(), ArrayUtils.EMPTY_OBJECT_ARRAY));
      assertFalse(q.iterator().hasNext());
      try {
        q.element();
        fail();
      } catch (NoSuchElementException success) {}
      try {
        q.iterator().next();
        fail();
      } catch (NoSuchElementException success) {}
      try {
        q.remove();
        fail();
      } catch (NoSuchElementException success) {}
    } catch (InterruptedException ie) {
      threadUnexpectedException(ie);
    }
  }

  byte[] serialBytes(Object o) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(o);
      oos.flush();
      oos.close();
      return bos.toByteArray();
    } catch (Throwable t) {
      threadUnexpectedException(t);
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
  }

  @SuppressWarnings("unchecked")
  <T> T serialClone(T o) {
    try {
      ObjectInputStream ois = new ObjectInputStream
          (new ByteArrayInputStream(serialBytes(o)));
      T clone = (T)ois.readObject();
      assertSame(o.getClass(), clone.getClass());
      return clone;
    } catch (Throwable t) {
      threadUnexpectedException(t);
      return null;
    }
  }

}
