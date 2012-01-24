/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.builtin;

public class AtomicInteger {

  private final Lock lock = new Lock();
  private int        value;

  public AtomicInteger(int initialValue) {
    this.value = initialValue;
  }

  public int addAndGet(int increment) {
    lock.writeLock();
    try {
      return value += increment;
    } finally {
      lock.writeUnlock();
    }
  }

  public int get() {
    lock.readLock();
    try {
      return value;
    } finally {
      lock.readUnlock();
    }
  }

  public int incrementAndGet() {
    lock.writeLock();
    try {
      return ++value;
    } finally {
      lock.writeUnlock();
    }
  }

  public void set(int i) {
    lock.writeLock();
    try {
      value = i;
    } finally {
      lock.writeUnlock();
    }
  }
}
