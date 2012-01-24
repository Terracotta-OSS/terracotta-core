/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.builtin;

public class AtomicReference<T> {

  private final Lock lock = new Lock();
  private T          value;

  public AtomicReference() {
    this(null);
  }

  public AtomicReference(T initialValue) {
    this.value = initialValue;
  }

  public T get() {
    lock.readLock();
    try {
      return value;
    } finally {
      lock.readUnlock();
    }
  }

  public void set(T v) {
    lock.writeLock();
    try {
      value = v;
    } finally {
      lock.writeUnlock();
    }
  }

  public boolean compareAndSet(T expect, T update) {
    lock.writeLock();
    try {
      if (value == null && expect == null) {
        value = update;
        return true;
      }

      if (value.equals(expect)) {
        value = update;
        return true;
      }

      return false;

    } finally {
      lock.writeUnlock();
    }
  }

  public T getAndSet(T v) {
    lock.writeLock();
    try {
      T rv = value;
      value = v;
      return rv;
    } finally {
      lock.writeUnlock();
    }
  }
}
