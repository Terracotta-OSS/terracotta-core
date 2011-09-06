/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package java.util.concurrent;

import com.tc.exception.TCRuntimeException;
import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.ManagerUtil;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class LinkedBlockingQueueTC<E> extends LinkedBlockingQueue<E> {

  private int capacity;
  private AtomicInteger count;  
  private Node head, last;
  private Condition notFull, notEmpty;
  private ReentrantLock putLock, takeLock;

  abstract void fullyLock();
  abstract void fullyUnlock();
  abstract void insert(E o);
  abstract E extract();
  abstract void signalNotEmpty();
  abstract void signalNotFull();

  @Override
  public boolean remove(Object o) {
    if (o == null) {
      return false;
    }
    int index = 0;
    boolean removed = false;

    fullyLock();
    try {
      Node<E> trail = head;
      Node<E> p = head.next;

      while (p != null) {
        if (o.equals(p.item)) {
          removed = true;
          break;
        }
        trail = p;
        p = p.next;        
        index++;
      }
      if (removed) {
        p.item = null;
        trail.next = p.next;
        if (ManagerUtil.isManaged(this)) {
          ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_AT_SIGNATURE, new Object[] {Integer.valueOf(index)});
        }
        if (last == p) {
          last = trail;
        }
        if (count.getAndDecrement() == capacity) {
          notFull.signalAll();
        }
      }
    } finally {
      fullyUnlock();
    }

    return removed;
  }

  @Override
  public boolean offer(E o) {
    if (o == null) {
      throw new NullPointerException();
    }

    if (count.get() >= capacity) {
      return false;
    }

    int c = -1;

    putLock.lock();
    try {
      if (count.get() < capacity) {
        insert(o);
        if (ManagerUtil.isManaged(this)) {
          ManagerUtil.logicalInvokeWithTransaction(this, putLock, SerializationUtil.QUEUE_PUT_SIGNATURE, new Object[] {o});
        }
        c = count.getAndIncrement();
        if (c + 1 < capacity) {
          notFull.signal();
        }
      }
    } finally {
      putLock.unlock();
    }

    if (c == 0) {
      signalNotEmpty();
    }

    return c >= 0;
  }

  @Override
  public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {    
    if (o == null) {
      throw new NullPointerException();
    }

    long nanos = unit.toNanos(timeout);

    int c = -1;

    putLock.lockInterruptibly();
    try {
      for (;;) {
        if (count.get() < capacity) {
          insert(o);
          if (ManagerUtil.isManaged(this)) {
            ManagerUtil.logicalInvokeWithTransaction(this, putLock, SerializationUtil.QUEUE_PUT_SIGNATURE, new Object[] {o});
          }
          c = count.getAndIncrement();
          if (c + 1 < capacity) {
            notFull.signal();
          }
          break;
        }
        if (nanos <= 0) {
          return false;
        }

        try {
          nanos = notFull.awaitNanos(nanos);
        } catch (InterruptedException ie) {
          notFull.signal();
          throw ie;
        }
      }
    } finally {
      putLock.unlock();
    }
    if (c == 0)
      signalNotEmpty();
    return true;
  }

  @Override
  public void put(E o) throws InterruptedException {
    if (o == null) {
      throw new NullPointerException();
    }

    int c = -1;

    putLock.lockInterruptibly();
    try {
      try {
        while (count.get() >= capacity) {
          notFull.await();
        }
      } catch (InterruptedException ie) {
        notFull.signal();
        throw ie;
      }
      insert(o);
      if (ManagerUtil.isManaged(this)) {
        ManagerUtil.logicalInvokeWithTransaction(this, putLock, SerializationUtil.QUEUE_PUT_SIGNATURE, new Object[] {o});
      }
      c = count.getAndIncrement();
      if (c + 1 < capacity) {
        notFull.signal();
      }
    } finally {
      putLock.unlock();
    }

    if (c == 0)
      signalNotEmpty();
  }

  public E __tc_take() {
    E x;
    int c = -1;

    takeLock.lock();
    try {
      if (head == last) {
        throw new TCRuntimeException("__tc_take: Trying to do a take from an empty queue");
      }
      x = __tc_extract();
      c = count.getAndDecrement();
    } finally {
      takeLock.unlock();
    }

    if (c == capacity) {
      signalNotFull();
    }

    return x;
  }

  private E __tc_extract() {
    Node<E> first = head.next;
    head = first;
    E x = first.item;
    first.item = null;
    return x;
  }
  
  public void __tc_put(E o) {
    if (o == null) {
      throw new NullPointerException();
    }

    int c = -1;

    putLock.lock();
    try {
      insert(o);
      c = count.getAndIncrement();

      if (c + 1 < capacity) {
        notFull.signal();
      }
    } finally {
      putLock.unlock();
    }

    if (c == 0) {
      signalNotEmpty();
    }
  }

  protected void init() {
    notEmpty = takeLock.newCondition();
    notFull = putLock.newCondition();
  }

  public E take() throws InterruptedException {
    E x;
    int c = -1;
    takeLock.lockInterruptibly();
    try {
      try {
        while (count.get() <= 0) {
          notEmpty.await();
        }
      } catch (InterruptedException ie) {
        notEmpty.signal(); // propagate to a non-interrupted thread
        throw ie;
      }

      x = extract();
      if (ManagerUtil.isManaged(this)) {
        ManagerUtil.logicalInvokeWithTransaction(this, takeLock, SerializationUtil.TAKE_SIGNATURE, new Object[] {});
      }
      c = count.getAndDecrement();
      if (c > 1)
        notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
    if (c == capacity)
      signalNotFull();
    return x;
  }

  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    E x = null;
    int c = -1;
    long nanos = unit.toNanos(timeout);
    takeLock.lockInterruptibly();
    try {
      for (;;) {
        if (count.get() > 0) {
          x = extract();
          if (ManagerUtil.isManaged(this)) {
            ManagerUtil.logicalInvokeWithTransaction(this, takeLock, SerializationUtil.TAKE_SIGNATURE, new Object[] {});
          }
          c = count.getAndDecrement();
          if (c > 1)
            notEmpty.signal();
          break;
        }
        if (nanos <= 0)
          return null;
        try {
          nanos = notEmpty.awaitNanos(nanos);
        } catch (InterruptedException ie) {
          notEmpty.signal(); // propagate to a non-interrupted thread
          throw ie;
        }
      }
    } finally {
      takeLock.unlock();
    }
    if (c == capacity)
      signalNotFull();
    return x;
  }

  public E poll() {
    if (count.get() <= 0) {
      return null;
    }
    
    E x = null;
    int c = -1;
    takeLock.lock();
    try {
      if (count.get() > 0) {
        x = extract();
        if (ManagerUtil.isManaged(this)) {
          ManagerUtil.logicalInvokeWithTransaction(this, takeLock, SerializationUtil.TAKE_SIGNATURE, new Object[] {});
        }
        c = count.getAndDecrement();
        if (c > 1)
          notEmpty.signal();
      }
    } finally {
      takeLock.unlock();
    }
    if (c == capacity)
      signalNotFull();
    return x;
  }

  public void clear() {
    fullyLock();
    try {
      head.next = null;
      last = head;
      if (ManagerUtil.isManaged(this)) {
        ManagerUtil.logicalInvoke(this, SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
      }
      if (count.getAndSet(0) == capacity)
        notFull.signalAll();
    } finally {
      fullyUnlock();
    }
  }

  public int drainTo(Collection<? super E> c) {
    if (c == null)
      throw new NullPointerException();
    if (c == this)
      throw new IllegalArgumentException();
    Node first;
    fullyLock();
    try {
      first = head.next;
      head.next = null;
      last = head;
      if (ManagerUtil.isManaged(this)) {
        ManagerUtil.logicalInvoke(this, SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
      }
      if (count.getAndSet(0) == capacity)
        notFull.signalAll();
    } finally {
      fullyUnlock();
    }
    // Transfer the elements outside of locks
    int n = 0;
    for (Node<E> p = first; p != null; p = p.next) {
      c.add(p.item);
      p.item = null;
      ++n;
    }
    return n;
  }

  public int drainTo(Collection<? super E> c, int maxElements) {
    if (c == null)
      throw new NullPointerException();
    if (c == this)
      throw new IllegalArgumentException();
    fullyLock();
    try {
      int n = 0;
      Node<E> p = head.next;
      while (p != null && n < maxElements) {
        c.add(p.item);
        p.item = null;
        p = p.next;
        ++n;
      }
      if (n != 0) {
        head.next = p;
        if (p == null)
          last = head;
        if (ManagerUtil.isManaged(this)) {
          ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_FIRST_N_SIGNATURE, new Object[] {Integer.valueOf(n)});
        }
        if (count.getAndAdd(-n) == capacity)
          notFull.signalAll();
      }
      return n;
    } finally {
      fullyUnlock();
    }
  }  
}
