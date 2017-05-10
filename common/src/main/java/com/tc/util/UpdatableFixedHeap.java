package com.tc.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is an implementation of a minheap data structure, which is prepropulated
 * at construction with the objects it holds. For each of these objects, a
 * wrapper object is created (and should after construction, be associated with
 * the payload object). After construction, the wrapper object may be used to inform
 * the heap that the underlying object has changed it's heap ordering value, in
 * either a toward-the-front or toward-the-end manner.
 *
 * The delta is then procesed ina thread safe manner, to rebalance the heap.
 *
 * In this way, the payload object with the smallest heap ordering value is available
 * by calling the top() method.
 *
 * @author cschanck
 **/
public class UpdatableFixedHeap<E extends UpdatableFixedHeap.Ordered> {

  private final AtomicIntegerArray locks;
  private final int arrayLength;
  private final boolean rigid;
  private AtomicReferenceArray<UpdatableWrapper<E>> arr;
  private volatile int size = 0;
  private ReentrantLock lock = new ReentrantLock(true);

  public UpdatableFixedHeap(boolean rigid, E[] elements) {
    this.rigid = rigid;
    arr = new AtomicReferenceArray<UpdatableWrapper<E>>(elements.length + 1);
    this.locks = new AtomicIntegerArray(arr.length());
    this.arrayLength = arr.length();
    for (E e : elements) {
      add(e);
    }
    reheap();
  }

  private boolean orderLock(int index1, int index2) {
    if (rigid) {
      return orderLockRigid(index1, index2);
    }
    return orderLockTry(index1, index2);
  }

  private boolean orderLock(int index1, int index2, int index3) {
    if (rigid) {
      return orderLockRigid(index1, index2, index3);
    }
    return orderLockTry(index1, index2, index3);
  }

  private boolean orderLockTry(int index1, int index2) {
    if (index1 < arrayLength) {
      if (!locks.compareAndSet(index1, 0, 1)) {
        return false;
      }
    }
    if (index2 < arrayLength) {
      if (!locks.compareAndSet(index2, 0, 1)) {
        if (index1 < arrayLength) {
          locks.compareAndSet(index1, 1, 0);
        }
        return false;
      }
    }
    return true;
  }

  private boolean orderLockRigid(int index1, int index2) {
    if (index1 < arrayLength) {
      while (!locks.compareAndSet(index1, 0, 1)) {
        ;
      }
    }
    if (index2 < arrayLength) {
      while (!locks.compareAndSet(index2, 0, 1)) {
        ;
      }
    }
    return true;
  }

  private boolean orderLockTry(int index1, int index2, int index3) {
    if (index1 < arrayLength) {
      if (!locks.compareAndSet(index1, 0, 1)) {
        return false;
      }
    }
    if (index2 < arrayLength) {
      if (!locks.compareAndSet(index2, 0, 1)) {
        if (index1 < arrayLength) {
          locks.compareAndSet(index1, 1, 0);
        }
        return false;
      }
    }
    if (index3 < arrayLength) {
      if (!locks.compareAndSet(index3, 0, 1)) {
        if (index1 < arrayLength) {
          locks.compareAndSet(index1, 1, 0);
        }
        if (index2 < arrayLength) {
          locks.compareAndSet(index2, 1, 0);
        }
        return false;
      }
    }
    return true;
  }

  private boolean orderLockRigid(int index1, int index2, int index3) {
    if (index1 < arrayLength) {
      while (!locks.compareAndSet(index1, 0, 1)) {
        ;
      }
    }
    if (index2 < arrayLength) {
      while (!locks.compareAndSet(index2, 0, 1)) {
        ;
      }
    }
    if (index3 < arrayLength) {
      while (!locks.compareAndSet(index3, 0, 1)) {
        ;
      }
    }
    return true;
  }

  private void orderUnlock(int index1, int index2) {
    if (index1 < arrayLength) {
      locks.compareAndSet(index1, 1, 0);
    }
    if (index2 < arrayLength) {
      locks.compareAndSet(index2, 1, 0);
    }
  }

  private void orderUnlock(int index1, int index2, int index3) {
    if (index1 < arrayLength) {
      locks.compareAndSet(index1, 1, 0);
    }
    if (index2 < arrayLength) {
      locks.compareAndSet(index2, 1, 0);
    }
    if (index3 < arrayLength) {
      locks.compareAndSet(index3, 1, 0);
    }
  }

  public int getSize() {
    return size;
  }

  private UpdatableWrapper<E> add(E e) {
    UpdatableWrapper<E> wrapper = new UpdatableWrapper<E>(e, ++size);
    arr.set(size, wrapper);
    siftUp(size);
    return wrapper;
  }

  private void reheap() {
    for (int i = size; i > 0; i--) {
      siftUp(i);
    }
  }

  public void possiblyCloserToFront(UpdatableWrapper<E> w) {
    siftUp(w.index);
  }

  public void possiblyCloserToEnd(UpdatableWrapper<E> w) {
    siftDown(w.index);
  }

  private boolean siftDown(int index) {
    int smallest = index;
    boolean workDone = false;
    while (true) {
      int next = siftDownOne(index);
      if (next > 0) {
        index = next;
        workDone = true;
      } else {
        break;
      }
    }
    return workDone;
  }

  private int siftDownOne(int index) {
    int ret = -1;
    boolean workDone = false;

    int smallest = index;
    int left = index * 2;
    int right = index * 2 + 1;

    if (left <= size) {
      if (arr.get(left).getHeapOrderingAmount() < arr.get(smallest).getHeapOrderingAmount()) {
        smallest = left;
      } else if (right <= size) {
        if (arr.get(right).getHeapOrderingAmount() < arr.get(smallest).getHeapOrderingAmount()) {
          smallest = right;
        }
      }
    } else if (right <= size) {
      if (arr.get(right).getHeapOrderingAmount() < arr.get(smallest).getHeapOrderingAmount()) {
        smallest = right;
      }
    }
    if (smallest != index) {
      int lock1 = index;
      int lock2 = smallest;
      if (orderLock(lock1, lock2)) {
        try {
          if (arr.get(index).getHeapOrderingAmount() > arr.get(smallest).getHeapOrderingAmount()) {
            swap(index, smallest);
            ret = smallest;
          }
        } finally {
          orderUnlock(lock1, lock2);
        }
      }
    }
    return ret;
  }

  private void siftUp(int index) {
    int parent = index / 2;
    while (parent > 0) {
      if (siftUpOne(parent, index)) {
        index = parent;
        parent = index / 2;
      } else {
        break;
      }
    }
  }

  private boolean siftUpOne(int parent, int index) {
    if (parent > 0) {
      int lock1 = parent;
      int lock2 = index;
      if (orderLock(lock1, lock2)) {
        try {
          if (arr.get(parent).getHeapOrderingAmount() > arr.get(index).getHeapOrderingAmount()) {
            swap(parent, index);
            return true;
          }
        } finally {
          orderUnlock(lock1, lock2);
        }
      }
    }
    return false;
  }

  private void swap(int parent, int index) {
    UpdatableWrapper swap = arr.get(parent);
    arr.set(parent, arr.get(index));
    arr.set(index, swap);
    arr.get(parent).index(parent);
    arr.get(index).index(index);
  }

  public String toString() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    _print(1, "", pw);
    pw.flush();
    return sw.toString();
  }

  void verify() {
    _verify(1);
  }

  private void _verify(int i) {
    if (i > size) {
      return;
    }
    if (arr.get(i).index != i) {
      throw new AssertionError("At index: " + i);
    }
    int n1 = 2 * i;
    if (n1 <= size) {
      if (arr.get(i).getHeapOrderingAmount() > arr.get(n1).getHeapOrderingAmount()) {
        throw new AssertionError("At index: " + i);
      }
      _verify(n1);
    }
    int n2 = n1 + 1;
    if (n2 <= size) {
      if (arr.get(i).getHeapOrderingAmount() > arr.get(n2).getHeapOrderingAmount()) {
        throw new AssertionError("At index: " + i);
      }
      _verify(n2);
    }
  }

  private void _print(int index, String indention, PrintWriter pw) {
    if (index > size) {
      return;
    }
    pw.println(indention + arr.get(index));
    int n1 = 2 * index;
    int n2 = n1 + 1;
    _print(n1, indention + "  ", pw);
    _print(n2, indention + "  ", pw);
  }

  public UpdatableWrapper top() {
    // empty
    if (size == 0) {
      return null;
    }
    return arr.get(1);
  }

  public UpdatableWrapper<E> wrapperFor(int index) {
    return arr.get(index + 1);
  }

  public static interface Ordered {
    int getHeapOrderingAmount();
  }

  public static class UpdatableWrapper<EE extends Ordered> {
    private final EE payload;
    private volatile int index;

    UpdatableWrapper(EE payload, int index) {
      this.payload = payload;
      this.index = index;
    }

    public int getHeapOrderingAmount() {
      return payload.getHeapOrderingAmount();
    }

    public EE getPayload() {
      return payload;
    }

    public int getIndex() {
      return index;
    }

    public UpdatableWrapper index(int index) {
      this.index = index;
      return this;
    }

    @Override
    public String toString() {
      return "Wrapper{" + "payload=" + payload + ", index=" + index + '}';
    }
  }
}
