/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.exception.TCRuntimeException;

public class Counter {
  private int count;

  public Counter() {
    this(0);
  }

  public Counter(int initial) {
    this.count = initial;
  }

  public int increment() {
    return increment(1);
  }

  public synchronized int increment(int val) {
    count += val;
    notifyAll();
    return count;
  }

  public int decrement() {
    return decrement(1);
  }

  public synchronized int decrement(int val) {
    count -= val;
    notifyAll();
    return count;
  }

  public synchronized int get() {
    return count;
  }
  
  public synchronized void reset() {
    reset(0);
  }
  
  public synchronized void reset(int initialValue) {
    this.count = initialValue;
    notifyAll();
  }

  public synchronized void waitUntil(int value) {
    while (this.count != value) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }
  }
}
