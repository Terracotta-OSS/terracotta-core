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

import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * An API compliance test for {@link BlockingQueue} interface.
 * The test is based on the original JSR166 blocking queue test.
 *
 * @author Eugene Shelestovich
 */
public abstract class BlockingQueueApiTest extends BaseConcurrentCollectionsTest {

  /**
   * Create a new instance of blocking queue with predefined capacity.
   * Override in subclasses for a particular {@link BlockingQueue} implementation.
   */
  protected abstract <E> BlockingQueue<E> emptyQueue(int capacity);

  private <E> BlockingQueue<E> emptyQueue() {
    return emptyQueue(SIZE);
  }

  /**
   * Returns a new queue of given size containing consecutive integers [0..n).
   */
  private BlockingQueue<Integer> populatedQueue(int n) {
    final BlockingQueue<Integer> q = emptyQueue(n);
    assertTrue(q.isEmpty());
    for (int i = 0; i < n; i++) {
      assertTrue(q.offer(i));
    }
    assertFalse(q.isEmpty());
    assertEquals(0, q.remainingCapacity());
    assertEquals(n, q.size());
    return q;
  }

  @Test(expected = NullPointerException.class)
  public void testShouldThrowNpeIfOfferNull() {
    final Queue<Object> q = emptyQueue();
    q.offer(null);
  }

  @Test(expected = NullPointerException.class)
  public void testShouldThrowNpeIfAddNull() {
    final Collection<Object> q = emptyQueue();
    q.add(null);
  }

  @Test
  public void testShouldThrowNpeIfTimedOfferNull() throws InterruptedException {
    final BlockingQueue<Object> q = emptyQueue();
    long startTime = System.nanoTime();
    try {
      q.offer(null, LONG_DELAY_MS, MILLISECONDS);
      fail();
    } catch (NullPointerException success) {}
    assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
  }

  @Test(expected = NullPointerException.class)
  public void testShouldThrowNpeIfPutNull() throws InterruptedException {
    final BlockingQueue<Object> q = emptyQueue();
    q.put(null);
  }

  @Test(expected = NullPointerException.class)
  public void testShouldThrowNpeIfAddAllNull() throws InterruptedException {
    final Collection<Object> q = emptyQueue();
    q.addAll(null);
  }

  @Test(expected = NullPointerException.class)
  public void testShouldThrowNpeIfAddAllNullElements() {
    final Collection<Integer> q = emptyQueue();
    final Collection<Integer> elements = Arrays.asList(new Integer[SIZE]);
    q.addAll(elements);
  }

  @Test(expected = NullPointerException.class)
  public void testShouldThrowNpeIfToArrayNull() {
    final Collection<Object> q = emptyQueue();
    q.toArray(null);
  }

  /**
   * A new queue has the indicated capacity
   */
  @Test
  public void testConstructor1() {
    assertEquals(SIZE, emptyQueue(SIZE).remainingCapacity());
  }

  /**
   * Constructor throws IAE if capacity argument nonpositive
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructor2() {
    emptyQueue(0);
  }

  /**
   * Queue transitions from empty to full when elements added
   */
  @Test
  public void testEmptyFull() {
    final BlockingQueue<Integer> q = emptyQueue(2);
    assertTrue(q.isEmpty());
    assertEquals(2, q.remainingCapacity());
    q.add(1);
    assertFalse(q.isEmpty());
    q.add(2);
    assertFalse(q.isEmpty());
    assertEquals(0, q.remainingCapacity());
    assertFalse(q.offer(3));
  }

  /**
   * remainingCapacity decreases on add, increases on remove
   */
  @Test
  public void testRemainingCapacity() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(i, q.remainingCapacity());
      assertEquals(SIZE - i, q.size());
      q.remove();
    }
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(SIZE - i, q.remainingCapacity());
      assertEquals(i, q.size());
      q.add(i);
    }
  }

  /**
   * Offer succeeds if not full; fails if full
   */
  @Test
  public void testOffer() {
    final BlockingQueue<Integer> q = emptyQueue(1);
    assertTrue(q.offer(0));
    assertFalse(q.offer(1));
  }

  /**
   * add succeeds if not full; throws ISE if full
   */
  @Test(expected = IllegalStateException.class)
  public void testAdd() {
    final BlockingQueue<Integer> q = emptyQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertTrue(q.add(i));
    }
    assertEquals(0, q.remainingCapacity());
    q.add(SIZE);
  }

  /**
   * addAll(this) throws IAE
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAddAllSelf() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    q.addAll(q);
  }

  /**
   * addAll of a collection with any null elements throws NPE after
   * possibly adding some elements
   */
  @Test(expected = NullPointerException.class)
  public void testAddAll3() {
    final BlockingQueue<Integer> q = emptyQueue(SIZE);
    Integer[] ints = new Integer[SIZE];
    for (int i = 0; i < SIZE - 1; ++i) {
      ints[i] = i;
    }
    q.addAll(Arrays.asList(ints));
  }

  /**
   * addAll throws ISE if not enough room
   */
  @Test(expected = IllegalStateException.class)
  public void testAddAll4() {
    final BlockingQueue<Integer> q = emptyQueue(1);
    Integer[] ints = new Integer[SIZE];
    for (int i = 0; i < SIZE; ++i) {
      ints[i] = i;
    }
    q.addAll(Arrays.asList(ints));
  }

  /**
   * Queue contains all elements, in traversal order, of successful addAll
   */
  @Test
  public void testAddAll5() {
    Integer[] empty = new Integer[0];
    Integer[] ints = new Integer[SIZE];
    for (int i = 0; i < SIZE; ++i) {
      ints[i] = i;
    }

    final BlockingQueue<Integer> q = emptyQueue(SIZE);
    assertFalse(q.addAll(Arrays.asList(empty)));
    assertTrue(q.addAll(Arrays.asList(ints)));
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(ints[i], q.poll());
    }
  }

  /**
   * all elements successfully put are contained
   */
  @Test
  public void testPut() throws InterruptedException {
    final BlockingQueue<Integer> q = emptyQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      q.put(i);
      assertTrue(q.contains(i));
    }
    assertEquals(0, q.remainingCapacity());
  }

  /**
   * peek returns next element, or null if empty
   */
  @Test
  public void testPeek() {
    final BlockingQueue q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(i, q.peek());
      assertEquals(i, q.poll());
      assertTrue(q.peek() == null || !q.peek().equals(i));
    }
    assertNull(q.peek());
  }

  /**
   * element returns next element, or throws NSEE if empty
   */
  @Test(expected = NoSuchElementException.class)
  public void testElement() {
    final BlockingQueue q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(i, q.element());
      assertEquals(i, q.poll());
    }
    q.element();
  }

  /**
   * remove removes next element, or throws NSEE if empty
   */
  @Test(expected = NoSuchElementException.class)
  public void testRemove() {
    final BlockingQueue q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(i, q.remove());
    }
    q.remove();
  }

  /**
   * contains(x) reports true when elements added but not yet removed
   */
  @Test
  public void testContains() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertTrue(q.contains(i));
      assertEquals(i, q.poll().intValue());
      assertFalse(q.contains(i));
    }
  }

  /**
   * clear removes all elements
   */
  @Test
  public void testClear() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    q.clear();
    assertTrue(q.isEmpty());
    assertEquals(0, q.size());
    assertEquals(SIZE, q.remainingCapacity());
    q.add(1);
    assertFalse(q.isEmpty());
    assertTrue(q.contains(1));
    q.clear();
    assertTrue(q.isEmpty());
  }

  /**
   * containsAll(c) is true when c contains a subset of elements
   */
  @Test
  public void testContainsAll() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    final BlockingQueue<Integer> p = emptyQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertTrue(q.containsAll(p));
      assertFalse(p.containsAll(q));
      p.add(i);
    }
    assertTrue(p.containsAll(q));
  }

  /**
   * retainAll(c) retains only those elements of c and reports true if changed
   */
  @Test
  public void testRetainAll() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    final BlockingQueue<Integer> p = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      boolean changed = q.retainAll(p);
      if (i == 0) {
        assertFalse(changed);
      } else {
        assertTrue(changed);
      }

      assertTrue(q.containsAll(p));
      assertEquals(SIZE - i, q.size());
      p.remove();
    }
  }

  /**
   * removeAll(c) removes only those elements of c and reports true if changed
   */
  @Test
  public void testRemoveAll() {
    for (int i = 1; i < SIZE; ++i) {
      final BlockingQueue<Integer> q = populatedQueue(SIZE);
      final BlockingQueue<Integer> p = populatedQueue(i);
      assertTrue(q.removeAll(p));
      assertEquals(SIZE - i, q.size());
      for (int j = 0; j < i; ++j) {
        assertFalse(q.contains(p.remove()));
      }
    }
  }

  /**
   * toArray contains all elements in FIFO order
   */
  @Test
  public void testToArray() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    for (final Object element : q.toArray()) {
      assertSame(element, q.poll());
    }
  }

  /**
   * toArray(a) contains all elements in FIFO order
   */
  @Test
  public void testToArray2() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    Integer[] ints = new Integer[SIZE];
    Integer[] array = q.toArray(ints);
    assertSame(ints, array);
    for (final Integer element : ints) {
      assertSame(element, q.poll());
    }
  }

  /**
   * toArray(incompatible array type) throws ArrayStoreException
   */
  @Test(expected = ArrayStoreException.class)
  public void testToArray1_BadArg() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    q.toArray(new String[10]);
  }

  /**
   * iterator iterates through all elements
   */
  @Test
  public void testIterator() throws InterruptedException {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    for (final Integer element : q) {
      assertEquals(element, q.take());
    }
  }

  /**
   * iterator.remove removes current element
   */
  @Test
  public void testIteratorRemove() {
    final BlockingQueue<Integer> q = emptyQueue(3);
    q.add(2);
    q.add(1);
    q.add(3);

    Iterator it = q.iterator();
    it.next();
    it.remove();

    it = q.iterator();
    assertSame(it.next(), 1);
    assertSame(it.next(), 3);
    assertFalse(it.hasNext());
  }

  /**
   * iterator ordering is FIFO
   */
  @Test
  public void testIteratorOrdering() {
    final BlockingQueue<Integer> q = emptyQueue(3);
    q.add(1);
    q.add(2);
    q.add(3);

    assertEquals("queue should be full", 0, q.remainingCapacity());

    int k = 0;
    for (int element : q) { //implicit iterator is here
      assertEquals(++k, element);
    }
    assertEquals(3, k);
  }

  /**
   * Modifications do not cause iterators to fail
   */
  @Test
  public void testWeaklyConsistentIteration() {
    final BlockingQueue<Integer> q = emptyQueue(3);
    q.add(1);
    q.add(2);
    q.add(3);

    for (Iterator it = q.iterator(); it.hasNext(); ) {
      q.remove();
      it.next();
    }
    assertEquals(0, q.size());
  }

  /**
   * toString contains toStrings of elements
   */
  @Test
  public void testToString() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    String s = q.toString();
    for (int i = 0; i < SIZE; ++i) {
      assertTrue(s.contains(String.valueOf(i)));
    }
  }

  /**
   * put blocks interruptibly if full
   */
  @Test
  public void testBlockingPut() throws InterruptedException {
    final BlockingQueue<Integer> q = emptyQueue(SIZE);
    final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        for (int i = 0; i < SIZE; ++i)
          q.put(i);
        assertEquals(SIZE, q.size());
        assertEquals(0, q.remainingCapacity());

        Thread.currentThread().interrupt();
        try {
          q.put(99);
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());

        pleaseInterrupt.countDown();
        try {
          q.put(99);
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());
      }
    });

    await(pleaseInterrupt);
    assertThreadStaysAlive(t);
    t.interrupt();
    awaitTermination(t);
    assertEquals(SIZE, q.size());
    assertEquals(0, q.remainingCapacity());
  }

  /**
   * put blocks interruptibly waiting for take when full
   */
  @Test
  public void testPutWithTake() throws InterruptedException {
    final int capacity = 2;
    final BlockingQueue<Integer> q = emptyQueue(capacity);
    final CountDownLatch pleaseTake = new CountDownLatch(1);
    final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        for (int i = 0; i < capacity; i++) {
          q.put(i);
        }
        pleaseTake.countDown();
        q.put(86);

        pleaseInterrupt.countDown();
        try {
          q.put(99);
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());
      }
    });

    await(pleaseTake);
    assertEquals(0, q.remainingCapacity());
    assertEquals(0, q.take().intValue());

    await(pleaseInterrupt);
    assertThreadStaysAlive(t);
    t.interrupt();
    awaitTermination(t);
    assertEquals(0, q.remainingCapacity());
  }

  /**
   * timed offer times out if full and elements not taken
   */
  @Test
  public void testTimedOffer() throws InterruptedException {
    final BlockingQueue<Object> q = emptyQueue(2);
    final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        q.put(new Object());
        q.put(new Object());
        long startTime = System.nanoTime();
        assertFalse(q.offer(new Object(), TIMEOUT_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= TIMEOUT_MS);
        pleaseInterrupt.countDown();
        try {
          q.offer(new Object(), 2 * LONG_DELAY_MS, MILLISECONDS);
          fail();
        } catch (InterruptedException success) {}
      }
    });

    await(pleaseInterrupt);
    assertThreadStaysAlive(t);
    t.interrupt();
    awaitTermination(t);
  }

  /**
   * take retrieves elements in FIFO order
   */
  @Test
  public void testTake() throws InterruptedException {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(i, q.take().intValue());
    }
  }

  /**
   * Take removes existing elements until empty, then blocks interruptibly
   */
  @Test
  public void testBlockingTake() throws InterruptedException {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(i, q.take().intValue());
        }

        Thread.currentThread().interrupt();
        try {
          q.take();
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());

        pleaseInterrupt.countDown();
        try {
          q.take();
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());
      }
    });

    await(pleaseInterrupt);
    assertThreadStaysAlive(t);
    t.interrupt();
    awaitTermination(t);
  }

  /**
   * poll succeeds unless empty
   */
  @Test
  public void testPoll() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(i, q.poll().intValue());
    }
    assertNull(q.poll());
  }

  /**
   * timed poll with zero timeout succeeds when non-empty, else times out
   */
  @Test
  public void testTimedPoll0() throws InterruptedException {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(i, q.poll(0, MILLISECONDS).intValue());
    }
    assertNull(q.poll(0, MILLISECONDS));
    checkEmpty(q);
  }

  /**
   * timed poll with nonzero timeout succeeds when non-empty, else times out
   */
  @Test
  public void testTimedPoll() throws InterruptedException {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    for (int i = 0; i < SIZE; ++i) {
      long startTime = System.nanoTime();
      assertEquals(i, q.poll(LONG_DELAY_MS, MILLISECONDS).intValue());
      assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
    }
    long startTime = System.nanoTime();
    assertNull(q.poll(TIMEOUT_MS, MILLISECONDS));
    assertTrue(millisElapsedSince(startTime) >= TIMEOUT_MS);
    checkEmpty(q);
  }

  /**
   * Interrupted timed poll throws InterruptedException instead of
   * returning timeout status
   */
  @Test
  public void testInterruptedTimedPoll() throws InterruptedException {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    final CountDownLatch aboutToWait = new CountDownLatch(1);
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        for (int i = 0; i < SIZE; ++i) {
          long t0 = System.nanoTime();
          assertEquals(i, (int)q.poll(LONG_DELAY_MS, MILLISECONDS));
          assertTrue(millisElapsedSince(t0) < SMALL_DELAY_MS);
        }
        long time = System.nanoTime();
        aboutToWait.countDown();
        try {
          q.poll(MEDIUM_DELAY_MS, MILLISECONDS);
          fail();
        } catch (InterruptedException success) {
          assertTrue(millisElapsedSince(time) < MEDIUM_DELAY_MS);
        }
      }
    });

    aboutToWait.await();
    waitForThreadToEnterWaitState(t, SMALL_DELAY_MS);
    t.interrupt();
    awaitTermination(t, MEDIUM_DELAY_MS);
    checkEmpty(q);
  }

  /**
   * timed poll before a delayed offer times out; after offer succeeds;
   * on interruption throws
   */
  @Test
  public void testTimedPollWithOffer() throws InterruptedException {
    final BlockingQueue<Integer> q = emptyQueue();
    final CheckedBarrier barrier = new CheckedBarrier(2);
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        long startTime = System.nanoTime();
        assertNull(q.poll(TIMEOUT_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= TIMEOUT_MS);

        barrier.await();

        assertSame(0, q.poll(LONG_DELAY_MS, MILLISECONDS));

        Thread.currentThread().interrupt();
        try {
          q.poll(LONG_DELAY_MS, MILLISECONDS);
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());

        barrier.await();
        try {
          q.poll(LONG_DELAY_MS, MILLISECONDS);
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());
      }
    });

    barrier.await();
    long startTime = System.nanoTime();
    assertTrue(q.offer(0, LONG_DELAY_MS, MILLISECONDS));
    assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);

    barrier.await();
    assertThreadStaysAlive(t);
    t.interrupt();
    awaitTermination(t);
  }

  /**
   * take() blocks interruptibly when empty
   */
  @Test
  public void testTakeFromEmptyBlocksInterruptibly() {
    final BlockingQueue<Integer> q = emptyQueue();
    final CountDownLatch threadStarted = new CountDownLatch(1);
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() {
        threadStarted.countDown();
        try {
          q.take();
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());
      }
    });

    await(threadStarted);
    assertThreadStaysAlive(t);
    t.interrupt();
    awaitTermination(t);
  }

  /**
   * take() throws InterruptedException immediately if interrupted
   * before waiting
   */
  @Test
  public void testTakeFromEmptyAfterInterrupt() {
    final BlockingQueue<Integer> q = emptyQueue();
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() {
        Thread.currentThread().interrupt();
        try {
          q.take();
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());
      }
    });

    awaitTermination(t);
  }

  /**
   * timed poll() blocks interruptibly when empty
   */
  @Test
  public void testTimedPollFromEmptyBlocksInterruptibly() {
    final BlockingQueue<Integer> q = emptyQueue();
    final CountDownLatch threadStarted = new CountDownLatch(1);
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() {
        threadStarted.countDown();
        try {
          q.poll(2 * LONG_DELAY_MS, MILLISECONDS);
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());
      }
    });

    await(threadStarted);
    assertThreadStaysAlive(t);
    t.interrupt();
    awaitTermination(t);
  }

  /**
   * timed poll() throws InterruptedException immediately if
   * interrupted before waiting
   */
  @Test
  public void testTimedPollFromEmptyAfterInterrupt() {
    final BlockingQueue<Integer> q = emptyQueue();
    Thread t = newStartedThread(new CheckedRunnable() {
      public void realRun() {
        Thread.currentThread().interrupt();
        try {
          q.poll(2 * LONG_DELAY_MS, MILLISECONDS);
          fail();
        } catch (InterruptedException success) {}
        assertFalse(Thread.interrupted());
      }
    });

    awaitTermination(t);
  }

  /**
   * remove(x) removes x and returns true if present
   */
  @Test
  public void testRemoveElement() {
    final BlockingQueue<Integer> q = emptyQueue();
    final int size = Math.min(q.remainingCapacity(), SIZE);
    final Integer[] elts = new Integer[size];
    assertFalse(q.contains(99));
    assertFalse(q.remove(99));
    checkEmpty(q);
    for (int i = 0; i < size; i++) {
      q.add(elts[i] = i);
    }
    for (int i = 1; i < size; i += 2) {
      for (int pass = 0; pass < 2; pass++) {
        assertEquals((pass == 0), q.contains(elts[i]));
        assertEquals((pass == 0), q.remove(elts[i]));
        assertFalse(q.contains(elts[i]));
        assertTrue(q.contains(elts[i - 1]));
        if (i < size - 1)
          assertTrue(q.contains(elts[i + 1]));
      }
    }
    if (size > 0) {
      assertTrue(q.contains(elts[0]));
    }
    for (int i = size - 2; i >= 0; i -= 2) {
      assertTrue(q.contains(elts[i]));
      assertFalse(q.contains(elts[i + 1]));
      assertTrue(q.remove(elts[i]));
      assertFalse(q.contains(elts[i]));
      assertFalse(q.remove(elts[i + 1]));
      assertFalse(q.contains(elts[i + 1]));
    }
    checkEmpty(q);
  }

  /**
   * offer transfers elements across Executor tasks
   */
  @Test
  public void testOfferInExecutor() {
    final BlockingQueue<Integer> q = emptyQueue(2);
    q.add(1);
    q.add(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    final CheckedBarrier threadsStarted = new CheckedBarrier(2);
    executor.execute(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        assertFalse(q.offer(3));
        threadsStarted.await();
        assertTrue(q.offer(3, LONG_DELAY_MS, MILLISECONDS));
        assertEquals(0, q.remainingCapacity());
      }
    });

    executor.execute(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        threadsStarted.await();
        assertEquals(0, q.remainingCapacity());
        assertSame(1, q.take());
      }
    });

    joinPool(executor);
  }

  /**
   * timed poll retrieves elements across Executor threads
   */
  @Test
  public void testPollInExecutor() {
    final BlockingQueue<Integer> q = emptyQueue(2);
    final CheckedBarrier threadsStarted = new CheckedBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    executor.execute(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        assertNull(q.poll());
        threadsStarted.await();
        assertSame(1, q.poll(LONG_DELAY_MS, MILLISECONDS));
        checkEmpty(q);
      }
    });

    executor.execute(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        threadsStarted.await();
        q.put(1);
      }
    });

    joinPool(executor);
  }

  /**
   * A deserialized serialized queue has same elements in same order
   */
  @Test
  public void testSerialization() throws Exception {
    Queue x = populatedQueue(SIZE);
    // not all implementations are serializable
    if (!(x instanceof Serializable)) return;

    Queue y = serialClone(x);

    assertTrue(x != y);
    assertEquals(x.size(), y.size());
    assertEquals(x.toString(), y.toString());
    assertTrue(Arrays.equals(x.toArray(), y.toArray()));
    while (!x.isEmpty()) {
      assertFalse(y.isEmpty());
      assertEquals(x.remove(), y.remove());
    }
    assertTrue(y.isEmpty());
  }

  /**
   * drainTo(c) empties queue into another collection c
   */
  @Test
  public void testDrainTo() {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    final List<Integer> l = new ArrayList<Integer>();
    q.drainTo(l);
    assertEquals(0, q.size());
    assertEquals(SIZE, l.size());
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(l.get(i), new Integer(i));
    }
    q.add(0);
    q.add(1);
    assertFalse(q.isEmpty());
    assertTrue(q.contains(0));
    assertTrue(q.contains(1));
    l.clear();
    q.drainTo(l);
    assertEquals(0, q.size());
    assertEquals(2, l.size());
    for (int i = 0; i < 2; ++i) {
      assertEquals(l.get(i), new Integer(i));
    }
  }

  /**
   * drainTo empties full queue, unblocking a waiting put.
   */
  @Test
  public void testDrainToWithActivePut() throws InterruptedException {
    final BlockingQueue<Integer> q = populatedQueue(SIZE);
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        q.put(SIZE + 1);
      }
    });

    t.start();
    List<Integer> l = new ArrayList<Integer>();
    q.drainTo(l);
    assertTrue(l.size() >= SIZE);
    for (int i = 0; i < SIZE; ++i) {
      assertEquals(l.get(i), new Integer(i));
    }
    t.join();
    assertTrue(q.size() + l.size() >= SIZE);
  }

  /**
   * drainTo(c, n) empties first min(n, size) elements of queue into c
   */
  @Test
  public void testDrainToN() {
    final BlockingQueue<Integer> q = emptyQueue(SIZE * 2);
    for (int i = 0; i < SIZE + 2; ++i) {
      for (int j = 0; j < SIZE; j++) {
        assertTrue(q.offer(j));
      }
      final List<Integer> l = new ArrayList<Integer>();
      q.drainTo(l, i);
      int k = (i < SIZE) ? i : SIZE;
      assertEquals(k, l.size());
      assertEquals(SIZE - k, q.size());
      for (int j = 0; j < k; ++j) {
        assertEquals(l.get(j), new Integer(j));
      }
      while (q.poll() != null) ;
    }
  }

  @Test(expected = NullPointerException.class)
  public void testShouldThrowNpeIfDrainToNull() {
    final BlockingQueue<Object> q = emptyQueue();
    q.drainTo(null);
  }

  @Test(expected = NullPointerException.class)
  public void testShouldThrowNpeIfDrainToNullN() {
    final BlockingQueue<Object> q = emptyQueue();
    q.drainTo(null, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShouldThrowIllegalArgumentIfDrainToSelf() {
    final BlockingQueue<Object> q = emptyQueue();
    q.drainTo(q);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShouldThrowIllegalArgumentIfDrainToSelfN() {
    final BlockingQueue<Object> q = emptyQueue();
    q.drainTo(q, 0);
  }

  @Test
  public void testShouldReturnZeroAndDoNothingIfDrainToNonPositiveMaxElements() {
    final BlockingQueue<Object> q = emptyQueue();
    final int[] ns = { 0, -1, -42, Integer.MIN_VALUE };
    q.add(1);
    final List<Object> c = new ArrayList<Object>();
    for (int n : ns) {
      assertEquals(0, q.drainTo(c, n));
    }
    assertEquals(1, q.size());
    assertSame(1, q.poll());
    assertTrue(c.isEmpty());
  }


}
