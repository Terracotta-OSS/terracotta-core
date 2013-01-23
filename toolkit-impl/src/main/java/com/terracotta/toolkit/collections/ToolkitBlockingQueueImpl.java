/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.tc.util.FindbugsSuppressWarnings;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class ToolkitBlockingQueueImpl<E> extends AbstractDestroyableToolkitObject<ToolkitBlockingQueue> implements
    ToolkitBlockingQueue<E>, RejoinAwareToolkitObject {
  private final int                  capacity;
  private final DestroyableToolkitList<E> backingList;
  private final ToolkitReadWriteLock lock;
  private final Condition            condition;

  public ToolkitBlockingQueueImpl(ToolkitObjectFactory factory, DestroyableToolkitList<E> backingList, int capacity) {
    super(factory);
    this.backingList = backingList;
    this.capacity = capacity;
    this.lock = backingList.getReadWriteLock();
    this.condition = lock.writeLock().getCondition();
  }

  @Override
  public void doRejoinStarted() {
    this.backingList.rejoinStarted();
  }

  @Override
  public void doRejoinCompleted() {
    this.backingList.rejoinCompleted();
  }

  @Override
  public void applyDestroy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doDestroy() {
    backingList.destroy();
    signalAll();
  }

  private void signalAll() {
    lock.writeLock().lock();
    try {
      this.condition.signalAll();
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public int getCapacity() {
    return capacity;
  }

  @Override
  public String getName() {
    return backingList.getName();
  }

  @Override
  public boolean offer(E e) {
    lock.writeLock().lock();
    try {
      int size = backingList.size();
      if (size < capacity) {
        boolean result = backingList.add(e);
        if (size == 0) {
          condition.signalAll();
        }
        return result;
      } else {
        return false;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void put(E e) throws InterruptedException {
    while (!offer(e, Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
      // keep trying until added successfully
    }
  }

  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    if (e == null) { throw new NullPointerException("null cannot be added to a BlockingQueue"); }
    TimeKeeper timer = new TimeKeeper(timeout, unit);
    do { // use do-while so timeout=0 also work
      if (lock.writeLock().tryLock(timer.getRemainingTimeNanos(), TimeUnit.NANOSECONDS)) {
        try {
          while (true) {
            int size = backingList.size();
            if (size < capacity) {
              boolean result = backingList.add(e);
              if (size == 0) {
                condition.signalAll();
              }
              return result;
            } else {
              long remaining = timer.getRemainingTimeNanos();
              if (remaining <= 0) { return false; }
              if (!condition.await(remaining, TimeUnit.NANOSECONDS)) {
                // if time elapsed before receiving signal, return false
                return false;
              }
            }
          }
        } finally {
          lock.writeLock().unlock();
        }
      }
    } while (timer.getRemainingTimeNanos() > 0);
    return false;
  }

  @Override
  public E take() throws InterruptedException {
    while (true) {
      // keep trying until successful
      E value = poll(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      if (value != null) { return value; }
    }
  }

  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    TimeKeeper timer = new TimeKeeper(timeout, unit);
    do { // use do-while so timeout=0 works
      if (lock.writeLock().tryLock(timer.getRemainingTimeNanos(), TimeUnit.NANOSECONDS)) {
        try {
          while (true) {
            int size = backingList.size();
            if (size > 0) {
              E value = backingList.remove(0);
              if (size == capacity) {
                condition.signalAll();
              }
              return value;
            } else {
              while (!condition.await(timer.getRemainingTimeNanos(), TimeUnit.NANOSECONDS)) {
                // time-elapsed
                return null;
              }
            }
          }
        } finally {
          lock.writeLock().unlock();
        }
      }
    } while (timer.getRemainingTimeNanos() > 0);
    return null;
  }

  @Override
  public int remainingCapacity() {
    lock.readLock().lock();
    try {
      return capacity - backingList.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int drainTo(Collection<? super E> c) {
    return drainTo(c, Integer.MAX_VALUE);
  }

  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    if (c == this) { throw new IllegalArgumentException(); }
    lock.writeLock().lock();
    int transferredCount = 0;
    try {
      // not optimized but will work
      for (int count = 0; backingList.size() > 0;) {
        E removed = backingList.remove(0);
        if (c.add(removed)) {
          transferredCount++;
        }
        if (++count >= maxElements) {
          break;
        }
      }

      if (transferredCount > 0) {
        condition.signalAll();
      }
      return transferredCount;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public E poll() {
    lock.writeLock().lock();
    try {
      int size = backingList.size();
      if (size > 0) {
        E value = backingList.remove(0);
        if (size == capacity) {
          condition.signalAll();
        }
        return value;
      } else {
        return null;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public E peek() {
    lock.writeLock().lock();
    try {
      if (backingList.size() > 0) {
        return backingList.get(0);
      } else {
        return null;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Iterator<E> iterator() {
    return backingList.iterator();
  }

  @Override
  public int size() {
    lock.readLock().lock();
    try {
      return backingList.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  private static class TimeKeeper {
    private final long start = System.nanoTime();

    private final long durationNanos;

    public TimeKeeper(long duration, TimeUnit unit) {
      this.durationNanos = unit.toNanos(duration);
    }

    public long getTimeElapsedNanos() {
      return System.nanoTime() - start;
    }

    public long getRemainingTimeNanos() {
      return Math.max(0, durationNanos - getTimeElapsedNanos());
    }

  }

  // from AbstractQueue, AbstractCollection

  @Override
  public boolean add(E e) {
    if (offer(e)) return true;
    else throw new IllegalStateException("Queue full");
  }

  /**
   * Retrieves and removes the head of this queue. This method differs from {@link #poll poll} only in that it throws an
   * exception if this queue is empty.
   * <p>
   * This implementation returns the result of <tt>poll</tt> unless the queue is empty.
   * 
   * @return the head of this queue
   * @throws NoSuchElementException if this queue is empty
   */
  @Override
  public E remove() {
    E x = poll();
    if (x != null) return x;
    else throw new NoSuchElementException();
  }

  /**
   * Retrieves, but does not remove, the head of this queue. This method differs from {@link #peek peek} only in that it
   * throws an exception if this queue is empty.
   * <p>
   * This implementation returns the result of <tt>peek</tt> unless the queue is empty.
   * 
   * @return the head of this queue
   * @throws NoSuchElementException if this queue is empty
   */
  @Override
  public E element() {
    E x = peek();
    if (x != null) return x;
    else throw new NoSuchElementException();
  }

  /**
   * Removes all of the elements from this queue. The queue will be empty after this call returns.
   * <p>
   * This implementation repeatedly invokes {@link #poll poll} until it returns <tt>null</tt>.
   */
  @Override
  public void clear() {
    while (poll() != null) {
      //
    }
    signalAll();
  }

  /**
   * TODO: Should we just add the remaining capacity
   */
  @Override
  public boolean addAll(Collection<? extends E> c) {
    lock.writeLock().lock();
    try {
      if (c == null) throw new NullPointerException();
      if (c == this) throw new IllegalArgumentException();
      if (c.size() > capacity) throw new IllegalArgumentException("Size cannot be greater than capacity: size="
                                                                  + c.size() + " , capacity=" + capacity);
      boolean interrupted = false;

      int sizeBefore = backingList.size();
      while (remainingCapacity() < c.size()) {
        try {
          this.condition.await();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
      boolean modified = backingList.addAll(c);

      if (interrupted) {
        Thread.currentThread().interrupt();
      }

      if (sizeBefore == 0) {
        signalAll();
      }

      return modified;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    return backingList.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return backingList.contains(o);
  }

  @Override
  public Object[] toArray() {
    return backingList.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return backingList.toArray(a);
  }

  @Override
  public boolean remove(Object o) {
    lock.writeLock().lock();
    try {
      boolean signal = remainingCapacity() == 0;
      boolean rv = backingList.remove(o);
      if (signal) {
        condition.signalAll();
      }
      return rv;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return backingList.containsAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    lock.writeLock().lock();
    try {
      boolean signal = remainingCapacity() == 0;

      boolean rv = backingList.removeAll(c);

      if (signal) {
        condition.signalAll();
      }
      return rv;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    lock.writeLock().lock();
    try {
      boolean signal = remainingCapacity() == 0;

      boolean rv = backingList.retainAll(c);
      if (signal) {
        condition.signalAll();
      }
      return rv;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  @FindbugsSuppressWarnings
  public String toString() {
    Iterator<E> i = iterator();
    if (!i.hasNext()) return "[]";

    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (;;) {
      E e = i.next();
      sb.append(e == this ? "(this Collection)" : e);
      if (!i.hasNext()) return sb.append(']').toString();
      sb.append(", ");
    }
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return backingList.getReadWriteLock();
  }
}
