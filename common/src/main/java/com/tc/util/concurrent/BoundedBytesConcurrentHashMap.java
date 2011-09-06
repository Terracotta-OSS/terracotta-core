/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.util.Util;

import java.util.concurrent.locks.Condition;

public abstract class BoundedBytesConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
  private final long maxBytes;
  private final long maxSegmentBytes;

  public BoundedBytesConcurrentHashMap(long maxBytes) {
    this(DEFAULT_INITIAL_CAPACITY, maxBytes);
  }

  public BoundedBytesConcurrentHashMap(int initialCapacity, long maxBytes) {
    this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, maxBytes);
  }

  public BoundedBytesConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, long maxBytes) {
    super(initialCapacity, loadFactor, concurrencyLevel);
    this.maxBytes = maxBytes;
    this.maxSegmentBytes = calculateSegmentBytesLimit(maxBytes);
  }

  private int calculateSegmentBytesLimit(long limit) {
    long segSize = (long) Math.floor((double) limit / segments.length);
    segSize = segSize <= 0 ? 1 : segSize;
    segSize = segSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : segSize;
    return (int) segSize;
  }

  public long getMaxSize() {
    return maxBytes;
  }

  // light weight; not consistent
  public long getSizeInBytes() {
    long currentBytes = 0;
    for (Segment<K, V> seg : segments) {
      currentBytes += ((BoundedBytesSegment) seg).bytes;
    }
    return currentBytes;
  }

  public abstract long getKeySize(K key);

  public abstract long getValueSize(V value);

  @Override
  protected Segment<K, V> createSegment(int cap, float loadFactor) {
    return new BoundedBytesSegment<K, V>(cap, loadFactor);
  }

  private final class BoundedBytesSegment<X, Y> extends Segment<X, Y> {
    private final Condition sizeFullCondition;
    private volatile long   bytes;

    BoundedBytesSegment(int initialCapacity, float lf) {
      super(initialCapacity, lf);
      sizeFullCondition = this.newCondition();
    }

    @Override
    protected void prePut() {
      // XXX: the current entry which is being put is not checked for the bytes limit.
      blockIfNecessary();
    }

    @Override
    protected void postPut(X key, Y oldValue, Y newValue) {
      if (oldValue == null) {
        // new addition
        bytes += (getKeySize((K) key) + getValueSizeChecked(newValue));
      } else {
        // value updation
        bytes += (getValueSizeChecked(newValue) - getValueSizeChecked(oldValue));
      }

    }

    @Override
    protected void postRemove(HashEntry<X, Y> oldEntry) {
      bytes -= (getKeySize((K) oldEntry.key) + getValueSizeChecked(oldEntry.value));
      unblockIfNecessary();
    }

    @Override
    protected void postReplace(X key, Y oldValue, Y newValue) {
      bytes += (getValueSizeChecked(newValue) - getValueSizeChecked(oldValue));
      unblockIfNecessary();
    }

    @Override
    protected void postClear(int countBefore) {
      bytes = 0;
      unblockIfNecessary();
    }

    private long getValueSizeChecked(Y value) {
      return (value != null ? getValueSize((V) value) : 0);
    }

    private void blockIfNecessary() {
      boolean isInterrupted = false;
      try {
        while (bytes >= maxSegmentBytes) {
          try {
            sizeFullCondition.await();
          } catch (InterruptedException e) {
            isInterrupted = true;
          }
        }
      } finally {
        if (isInterrupted) {
          Util.selfInterruptIfNeeded(isInterrupted);
        }
      }
    }

    private void unblockIfNecessary() {
      if (bytes < maxSegmentBytes) {
        sizeFullCondition.signalAll();
      }
    }
  }
}
