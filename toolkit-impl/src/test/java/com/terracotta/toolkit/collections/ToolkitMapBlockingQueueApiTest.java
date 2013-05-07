/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.terracotta.toolkit.collections;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Shelestovich
 */
public class ToolkitMapBlockingQueueApiTest extends BlockingQueueApiTest {

  @Override
  protected <E> BlockingQueue<E> emptyQueue(final int capacity) {
    return new ToolkitMapBlockingQueue<E>("testQueue", capacity,
        new MockToolkitStore<String, E>("testQueue_store"),
        new MockToolkitReadWriteLock("testQueue_lock"));
  }

  /**
   * Initializing from null Collection throws NPE
   */
  @Test(expected = NullPointerException.class)
  public void testConstructor3() {
    new ToolkitMapBlockingQueue<Object>("testQueue", SIZE, null,
        new MockToolkitStore<String, Object>("testQueue_store"),
        new MockToolkitReadWriteLock("testQueue_lock"));
  }

  /**
   * Initializing from Collection of null elements throws NPE
   */
  @Test(expected = NullPointerException.class)
  public void testConstructor4() {
    Collection<Integer> elements = Arrays.asList(new Integer[SIZE]);
    new ToolkitMapBlockingQueue<Object>("testQueue", SIZE, elements,
        new MockToolkitStore<String, Object>("testQueue_store"),
        new MockToolkitReadWriteLock("testQueue_lock"));
  }

  /**
   * Initializing from Collection with some null elements throws NPE
   */
  @Test(expected = NullPointerException.class)
  public void testConstructor5() {
    Integer[] ints = new Integer[SIZE];
    for (int i = 0; i < SIZE - 1; ++i) {
      ints[i] = i;
    }
    new ToolkitMapBlockingQueue<Object>("testQueue", SIZE, Arrays.asList(ints),
        new MockToolkitStore<String, Object>("testQueue_store"),
        new MockToolkitReadWriteLock("testQueue_lock"));
  }

  /**
   * Initializing from too large collection throws IAE
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructor6() {
    Integer[] ints = new Integer[SIZE];
    for (int i = 0; i < SIZE; ++i) {
      ints[i] = i;
    }
    Collection<Integer> elements = Arrays.asList(ints);
    new ToolkitMapBlockingQueue<Object>("testQueue", SIZE - 1, elements,
        new MockToolkitStore<String, Object>("testQueue_store"),
        new MockToolkitReadWriteLock("testQueue_lock"));
  }

  /**
   * Queue contains all elements of collection used to initialize
   */
  @Test
  public void testConstructor7() {
    Integer[] ints = new Integer[SIZE];
    for (int i = 0; i < SIZE; ++i) {
      ints[i] = i;
    }
    final Collection<Integer> elements = Arrays.asList(ints);
    final BlockingQueue<Integer> q = new ToolkitMapBlockingQueue<Integer>(
        "testQueue", SIZE, elements,
        new MockToolkitStore<String, Integer>("testQueue_store"),
        new MockToolkitReadWriteLock("testQueue_lock"));

    for (int i = 0; i < SIZE; ++i) {
      assertEquals(ints[i], q.poll());
    }
  }

  @Test
  public void testSequentialAdd() throws Exception {
    final BlockingQueue<Integer> q = new ToolkitMapBlockingQueue<Integer>(
        "testQueue", new MockToolkitStore<String, Integer>("testQueue_store"),
        new MockToolkitReadWriteLock("testQueue_lock"));

    for (int i = 0; i < SIZE; i++) {
      q.add(i);
    }
    for (int i = 0; i < SIZE; i++) {
      assertEquals(i, q.take().intValue());
    }
  }

}
