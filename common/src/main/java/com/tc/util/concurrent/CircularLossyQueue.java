/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CircularLossyQueue<T> {
  private final AtomicReference<T>[] circularArray;
  private final int                  maxSize;

  private final AtomicLong           currentIndex = new AtomicLong(-1);

  public CircularLossyQueue(int size) {
    this.circularArray = initRefArray(size);
    for (int i = 0; i < size; i++) {
      this.circularArray[i] = new AtomicReference<>();
    }
    this.maxSize = size;
  }

  @SuppressWarnings("unchecked")
  private AtomicReference<T>[] initRefArray(int size) {
    return (AtomicReference<T>[]) new AtomicReference<?>[size];
  }

  public T push(T newVal) {
    int index = (int) (currentIndex.incrementAndGet() % maxSize);
    AtomicReference<T> ref = circularArray[index];
    T oldVal = ref.get();
    ref.set(newVal);
    return oldVal;
  }

  public T[] toArray(T[] type) {
    if (type.length > maxSize) { throw new IllegalArgumentException("Size of array passed in cannot be greater than "
                                                                    + maxSize); }

    int curIndex = getCurrentIndex();
    for (int k = 0; k < type.length; k++) {
      int index = getIndex(curIndex - k);
      type[k] = circularArray[index].get();
    }
    return type;
  }

  private int getIndex(int index) {
    index = index < 0 ? index + maxSize : index;
    return index;
  }

  public T peek() {
    if (depth() == 0) return null;
    return circularArray[getIndex(getCurrentIndex())].get();
  }

  public boolean isEmtpy() {
    return depth() == 0;
  }

  private int getCurrentIndex() {
    return (int) (currentIndex.get() % maxSize);
  }

  public int depth() {
    long currInd = currentIndex.get() + 1;
    return currInd >= maxSize ? maxSize : (int) currInd;
  }
}
