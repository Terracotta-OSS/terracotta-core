/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStore;

import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * A distributed bounded blocking queue implementation based on the clustered map {@link ToolkitStore}. The map access
 * pattern is similar to an circular indexing array <tt>[0..capacity - 1]</tt>.
 * 
 * @author Eugene Shelestovich
 * @see ToolkitBlockingQueue
 * @see java.util.concurrent.ArrayBlockingQueue
 */
public class ToolkitMapBlockingQueue<E> implements ToolkitBlockingQueue<E>, RejoinAwareToolkitObject {

  // store head, tail and capacity in the same map to make it more compact
  private static final String                   HEAD_KEY              = "__head";
  // points to the next free slot immediately following the last occupied element
  private static final String                   TAIL_KEY              = "__tail";
  private static final String                   CAPACITY_KEY          = "__capacity";

  private static final int                      RESERVED_FIELDS_COUNT = 3;

  private final ToolkitReadWriteLock            lock;
  // we have to use a single condition here,
  // because ToolkitLock doesn't support multiple conditions
  private final Condition                       notEmptyOrFull;

  private final String                          name;
  // to access unlocked store operations
  private final ToolkitCacheInternal<String, E> map;
  // capacity is immutable, so let's cache it locally
  private final int                             capacity;

  public ToolkitMapBlockingQueue(final String name, final ToolkitStore<String, E> map, final ToolkitReadWriteLock lock) {
    this(name, Integer.MAX_VALUE, map, lock);
  }

  public ToolkitMapBlockingQueue(final String name, final int capacity, final Collection<? extends E> c,
                                 final ToolkitStore<String, E> map, final ToolkitReadWriteLock lock) {
    this(name, capacity, map, lock);

    if (capacity < c.size()) throw new IllegalArgumentException("Queue capacity " + capacity
                                                                + " is less than input collection size " + c.size());

    for (final E element : c) {
      add(element);
    }
  }

  public ToolkitMapBlockingQueue(final String name, final int capacity, final ToolkitStore<String, E> map,
                                 final ToolkitReadWriteLock lock) {
    if (capacity <= 0) throw new IllegalArgumentException("Capacity should be a positive integer");
    if (map == null) throw new NullPointerException("Store is not specified");
    if (lock == null) throw new NullPointerException("Lock is not specified");

    this.map = (ToolkitCacheInternal<String, E>) map;
    this.name = name;
    this.lock = lock;
    this.notEmptyOrFull = writeLock().getCondition();
    this.capacity = capacity;
    final Integer oldCapacity = (Integer) map.get(CAPACITY_KEY);
    if (oldCapacity == null) {
      initNewMap();
    } else {
      if (capacity != oldCapacity) { throw new IllegalArgumentException("A "
                                                                        + ToolkitMapBlockingQueue.class.getSimpleName()
                                                                        + " with name '" + name
                                                                        + "' already exists with different capacity - "
                                                                        + oldCapacity + ", requested capacity - "
                                                                        + capacity); }
    }
  }

  // to get rid of internal store lock
  private void unlockedPutNoReturn(String key, E value) {
    map.unlockedPutNoReturn(key, value, 0, 0, 0);
  }

  private E unlockedGet(String key) {
    return map.unlockedGet(key, true);
  }

  private void unlockedRemoveNoReturn(String key) {
    map.unlockedRemoveNoReturn(key);
  }

  private void initNewMap() {
    writeLock().lock();
    try {
      // double checked locking to prevent initialization race
      if (map.get(CAPACITY_KEY) != null) { return; }

      unlockedPutNoReturn(HEAD_KEY, (E) Integer.valueOf(0));
      unlockedPutNoReturn(TAIL_KEY, (E) Integer.valueOf(0));
      unlockedPutNoReturn(CAPACITY_KEY, (E) Integer.valueOf(capacity));
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public final int getCapacity() {
    return capacity;
  }

  private int getHead() {
    return (Integer) unlockedGet(HEAD_KEY);
  }

  private void setHead(int head) {
    unlockedPutNoReturn(HEAD_KEY, (E) Integer.valueOf(head));
  }

  private int getTail() {
    return (Integer) unlockedGet(TAIL_KEY);
  }

  private void setTail(int tail) {
    unlockedPutNoReturn(TAIL_KEY, (E) Integer.valueOf(tail));
  }

  // this is supposed to be inlined by JIT
  private ToolkitLock writeLock() {
    return lock.writeLock();
  }

  private ToolkitLock readLock() {
    return lock.readLock();
  }

  /**
   * {@link ToolkitStore} currently supports only {@link String} keys, so we have to use some kind of converter.
   */
  private static String toKey(int i) {
    return String.valueOf(i);
  }

  @Override
  public int size() {
    readLock().lock();
    try {
      // ignore reserved fields
      final int size = map.size() - RESERVED_FIELDS_COUNT;
      // should never happen, but paranoid programming is good
      if (size < 0) { throw new IllegalStateException("Queue size is negative"); }
      return size;
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public int remainingCapacity() {
    readLock().lock();
    try {
      return capacity - size();
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public final boolean add(final E e) {
    writeLock().lock();
    try {
      if (offer(e)) {
        return true;
      } else {
        throw new IllegalStateException("Queue is full");
      }
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public boolean addAll(final Collection<? extends E> c) {
    if (c == null) throw new NullPointerException();
    if (c == this) throw new IllegalArgumentException();

    writeLock().lock();
    try {
      boolean modified = false;
      for (final E element : c) {
        if (add(element)) {
          modified = true;
        }
      }
      return modified;
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public boolean offer(E e) {
    if (e == null) throw new NullPointerException();

    writeLock().lock();
    try {
      if (isFull()) {
        return false;
      } else {
        insert(e);
        return true;
      }
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    if (e == null) throw new NullPointerException();

    long nanos = unit.toNanos(timeout);
    writeLock().lockInterruptibly();
    try {
      while (true) {
        if (isNotFull()) {
          insert(e);
          return true;
        }
        if (nanos <= 0) return false;
        try {
          nanos = notEmptyOrFull.awaitNanos(nanos);
        } catch (InterruptedException ie) {
          // propagate to non-interrupted thread
          notEmptyOrFull.signalAll();
          throw ie;
        }
      }
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();

    writeLock().lockInterruptibly();
    try {
      try {
        while (isFull()) {
          notEmptyOrFull.await();
        }
      } catch (InterruptedException ie) {
        // propagate to non-interrupted thread
        notEmptyOrFull.signalAll();
        throw ie;
      }
      insert(e);
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public E element() {
    readLock().lock();
    try {
      final E x = peek();
      if (x != null) {
        return x;
      } else {
        throw new NoSuchElementException();
      }
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public E poll() {
    writeLock().lock();
    try {
      return isEmpty() ? null : extract();
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    writeLock().lockInterruptibly();
    try {
      while (true) {
        if (isNotEmpty()) { return extract(); }
        if (nanos <= 0) return null;

        try {
          nanos = notEmptyOrFull.awaitNanos(nanos);
        } catch (InterruptedException ie) {
          // propagate to non-interrupted thread
          notEmptyOrFull.signalAll();
          throw ie;
        }
      }
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public E take() throws InterruptedException {
    writeLock().lockInterruptibly();
    try {
      try {
        while (isEmpty()) {
          notEmptyOrFull.await();
        }
      } catch (InterruptedException ie) {
        // propagate to non-interrupted thread
        notEmptyOrFull.signalAll();
        throw ie;
      }
      return extract();
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public E remove() {
    writeLock().lock();
    try {
      final E x = poll();
      if (x != null) {
        return x;
      } else {
        throw new NoSuchElementException();
      }
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public E peek() {
    readLock().lock();
    try {
      return (isEmpty()) ? null : unlockedGet(toKey(getHead()));
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public int drainTo(Collection<? super E> c) {
    if (c == null) throw new NullPointerException();
    if (c == this) throw new IllegalArgumentException();

    writeLock().lock();
    try {
      int i = getHead(); // an index of currently draining element
      int n = 0; // a number of elements drained
      final int size = size();
      while (n < size) {
        final String key = toKey(i);
        c.add(unlockedGet(key));
        unlockedRemoveNoReturn(key);
        i = increment(i);
        ++n;
      }
      if (n > 0) { // if something was drained at all
        setHead(0);
        setTail(0);
        notEmptyOrFull.signalAll();
      }
      return n;
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    if (c == null) throw new NullPointerException();
    if (c == this) throw new IllegalArgumentException();
    if (maxElements <= 0) return 0;

    writeLock().lock();
    try {
      int i = getHead();
      int n = 0;
      final int size = size();
      int max = (maxElements < size) ? maxElements : size;
      while (n < max) {
        final String key = toKey(i);
        c.add(unlockedGet(key));
        unlockedRemoveNoReturn(key);
        i = increment(i);
        ++n;
      }
      if (n > 0) {
        setHead(i);
        notEmptyOrFull.signalAll();
      }
      return n;
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public boolean remove(final Object o) {
    if (o == null) return false;

    writeLock().lock();
    try {
      int i = getHead(); // the current element to compare
      int k = 0;
      final int size = size();
      while (true) {
        if (k++ >= size) return false; // have we already passed thru all elements ?

        if (o.equals(unlockedGet(toKey(i)))) {
          removeAt(i);
          return true;
        }
        i = increment(i);
      }
    } finally {
      writeLock().unlock();
    }
  }

  /**
   * Delete item at position i. Should be invoked under lock.
   */
  private void removeAt(int i) {
    int localHead = getHead();
    // if removing front item, just advance
    if (i == localHead) {
      unlockedRemoveNoReturn(toKey(localHead));
      setHead(increment(localHead));
    } else {
      // slide over all others up through tail
      while (true) {
        int next = increment(i);
        if (next != getTail()) {
          unlockedPutNoReturn(toKey(i), unlockedGet(toKey(next))); // usually replace
          i = next;
        } else {
          unlockedRemoveNoReturn(toKey(i));
          setTail(i);
          break;
        }
      }
    }
    notEmptyOrFull.signalAll();
  }

  /**
   * {@inheritDoc}
   * <p>
   * This operation is atomic.
   */
  @Override
  public boolean removeAll(final Collection<?> c) {
    writeLock().lock();
    try {
      boolean modified = false;
      Iterator<?> e = iterator();
      while (e.hasNext()) {
        if (c.contains(e.next())) {
          e.remove();
          modified = true;
        }
      }
      // notify only if something changes
      if (modified) {
        notEmptyOrFull.signalAll();
      }
      return modified;
    } finally {
      writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * This operation is atomic.
   */
  @Override
  public boolean retainAll(final Collection<?> c) {
    writeLock().lock();
    try {
      boolean modified = false;
      Iterator<E> e = iterator();
      while (e.hasNext()) {
        if (!c.contains(e.next())) {
          e.remove();
          modified = true;
        }
      }
      // notify only if something changes
      if (modified) {
        notEmptyOrFull.signalAll();
      }
      return modified;
    } finally {
      writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * This implementation repeatedly invokes {@link #poll poll} until it returns <tt>null</tt>. This operation is atomic.
   */
  @Override
  public void clear() {
    writeLock().lock();
    try {
      final int size = size();
      int i = getHead(); // index of the current element
      int k = size;
      while (k-- > 0) {
        unlockedRemoveNoReturn(toKey(i));
        i = increment(i);
      }
      setTail(0);
      setHead(0);
      notEmptyOrFull.signalAll();
    } finally {
      writeLock().unlock();
    }
  }

  @Override
  public Object[] toArray() {
    readLock().lock();
    try {
      final int size = size();
      final Object[] a = new Object[size];
      int k = 0;
      int i = getHead();
      while (k < size) {
        a[k++] = unlockedGet(toKey(i));
        i = increment(i);
      }
      return a;
    } finally {
      readLock().unlock();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) {
    readLock().lock();
    try {
      final int size = size();
      if (a.length < size) {
        a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
      }

      int k = 0;
      int i = getHead();
      while (k < size) {
        a[k++] = (T) unlockedGet(toKey(i));
        i = increment(i);
      }
      if (a.length > size) {
        a[size] = null;
      }
      return a;
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public boolean contains(final Object o) {
    if (o == null) return false;

    readLock().lock();
    try {
      final int size = size();
      int i = getHead(); // index of the current element
      int k = 0; // number of visited elements
      while (k++ < size) {
        if (o.equals(unlockedGet(toKey(i)))) { return true; }
        i = increment(i);
      }
      return false;
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public boolean containsAll(final Collection<?> c) {
    readLock().lock();
    try {
      for (final Object element : c) {
        if (!contains(element)) { return false; }
      }
      return true;
    } finally {
      readLock().unlock();
    }
  }

  private boolean isFull() {
    readLock().lock();
    try {
      return capacity == size();
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    readLock().lock();
    try {
      return size() == 0;
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public String toString() {
    final Iterator<E> i = iterator();
    if (!i.hasNext()) return "[]";

    final StringBuilder sb = new StringBuilder();
    sb.append('[');
    while (true) {
      final E e = i.next();
      sb.append(e == this ? "(this Collection)" : e);
      if (!i.hasNext()) return sb.append(']').toString();
      sb.append(", ");
    }
  }

  private boolean isNotFull() {
    return !isFull();
  }

  private boolean isNotEmpty() {
    return !isEmpty();
  }

  /**
   * Inserts element at the end of the queue. Should be invoked under lock.
   */
  private void insert(final E e) {
    final int localTail = getTail();
    unlockedPutNoReturn(toKey(localTail), e);
    // cyclic indexing
    setTail(increment(localTail));
    // the queue becomes not empty
    notEmptyOrFull.signalAll();
  }

  /**
   * Extracts element from the head of the queue. Should be invoked under lock.
   */
  private E extract() {
    final int localHead = getHead();
    final String key = toKey(localHead);
    final E value = unlockedGet(key);
    unlockedRemoveNoReturn(key);
    setHead(increment(localHead));
    // the queue is not full anymore
    notEmptyOrFull.signalAll();
    return value;
  }

  /**
   * Circular increment. Should be invoked under lock.
   */
  private int increment(int i) {
    return (++i == capacity) ? 0 : i;
  }

  /**
   * Circular decrement. Should be invoked under lock.
   */
  private int decrement(int i) {
    return ((i == 0) ? capacity : i) - 1;
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return lock;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Returns an iterator over the elements in this queue in proper sequence. The returned <tt>Iterator</tt> is a
   * "weakly consistent" iterator that will never throw {@link java.util.ConcurrentModificationException}, and
   * guarantees to traverse elements as they existed upon construction of the iterator, and may (but is not guaranteed
   * to) reflect any modifications subsequent to construction.
   * 
   * @return an iterator over the elements in this queue in proper sequence
   */
  @Override
  public Iterator<E> iterator() {
    readLock().lock();
    try {
      return new SimpleMapBlockingQueueIterator();
    } finally {
      readLock().unlock();
    }
  }

  @Override
  public boolean isDestroyed() {
    return map.isDestroyed();
  }

  @Override
  public void destroy() {
    map.destroy();
  }

  @Override
  public void rejoinStarted() {
    //
  }

  @Override
  public void rejoinCompleted() {
    //
  }

  private final class SimpleMapBlockingQueueIterator implements Iterator<E> {

    private int remaining;   // number of elements yet to be returned
    private int nextIndex;   // index of element to be returned by next() call
    private E   nextItem;    // element to be returned by next call to next
    private E   lastItem;    // element returned by last call to next
    private int lastReturned; // index of last element returned, or -1 if none

    private SimpleMapBlockingQueueIterator() {
      remaining = size();
      nextIndex = getHead();
      lastReturned = -1;
      if (remaining > 0) {
        nextItem = unlockedGet(toKey(nextIndex));
      }
    }

    @Override
    public boolean hasNext() {
      return remaining > 0;
    }

    @Override
    public E next() {
      if (remaining <= 0) throw new NoSuchElementException();

      readLock().lock();
      try {
        lastReturned = nextIndex;
        lastItem = nextItem;
        E x = nextItem;
        while (--remaining > 0) { // skip null entries
          nextIndex = increment(nextIndex);
          nextItem = unlockedGet(toKey(nextIndex));
          if (nextItem != null) break; // next element found
        }
        return x;
      } finally {
        readLock().unlock();
      }
    }

    @Override
    public void remove() {
      writeLock().lock();
      try {
        // call remove() only after next()
        int i = lastReturned;
        if (i == -1) throw new IllegalStateException("remove() should only be invoked after next(). "
                                                     + "This call can only be made once per call to next()");
        lastReturned = -1;

        final E x = lastItem;
        lastItem = null;
        // only remove if item still at index
        if (x == unlockedGet(toKey(i))) {
          boolean shouldRemoveHead = (i == getHead());
          removeAt(i);
          if (!shouldRemoveHead) {
            nextIndex = decrement(nextIndex);
          }
        }
      } finally {
        writeLock().unlock();
      }
    }
  }

}
