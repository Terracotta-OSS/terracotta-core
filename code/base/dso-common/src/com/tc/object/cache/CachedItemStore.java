/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.locks.LockID;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class CachedItemStore {

  static final int                            MAX_SEGMENTS     = 1 << 16;
  static final int                            MAXIMUM_CAPACITY = 1 << 30;

  private final int                           segmentShift;
  private final int                           segmentMask;
  private final HashMap<LockID, CachedItem>[] segments;
  private final ReentrantLock[]               locks;

  public CachedItemStore(int initialCapacity, final float loadFactor, int concurrencyLevel) {
    if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0) { throw new IllegalArgumentException(); }

    if (concurrencyLevel > MAX_SEGMENTS) {
      concurrencyLevel = MAX_SEGMENTS;
    }

    // Find power-of-two sizes best matching arguments
    int sshift = 0;
    int ssize = 1;
    while (ssize < concurrencyLevel) {
      ++sshift;
      ssize <<= 1;
    }
    this.segmentShift = 32 - sshift;
    this.segmentMask = ssize - 1;
    this.segments = new HashMap[ssize];
    this.locks = new ReentrantLock[ssize];

    if (initialCapacity > MAXIMUM_CAPACITY) {
      initialCapacity = MAXIMUM_CAPACITY;
    }
    int c = initialCapacity / ssize;
    if (c * ssize < initialCapacity) {
      ++c;
    }
    int cap = 1;
    while (cap < c) {
      cap <<= 1;
    }

    for (int i = 0; i < this.segments.length; ++i) {
      this.segments[i] = new HashMap<LockID, CachedItem>(cap, loadFactor);
      this.locks[i] = new ReentrantLock();
    }
  }

  /**
   * Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions. This
   * is critical because CachedItemStore uses power-of-two length hash tables, that otherwise encounter collisions for
   * hashCodes that do not differ in lower or upper bits.
   */
  private static int hash(int h) {
    // Spread bits to regularize both segment and index locations,
    // using variant of single-word Wang/Jenkins hash.
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  /**
   * Returns the segment that should be used for key with given hash
   * 
   * @param hash the hash code for the key
   * @return the segment
   */
  final int segmentFor(final int hash) {
    return (hash >>> this.segmentShift) & this.segmentMask;
  }

  // For tests
  CachedItem get(final LockID lockID) {
    final int hash = hash(lockID.hashCode());
    final int index = segmentFor(hash);

    this.locks[index].lock();
    try {
      return this.segments[index].get(lockID);
    } finally {
      this.locks[index].unlock();
    }
  }

  public void add(final LockID lockID, final CachedItem item) {
    if (item == null) { throw new NullPointerException(); }
    final int hash = hash(lockID.hashCode());
    final int index = segmentFor(hash);

    this.locks[index].lock();
    try {
      final CachedItem old = this.segments[index].put(lockID, item);
      if (old != null) {
        item.setNext(old);
      }
    } finally {
      this.locks[index].unlock();
    }
  }

  public void remove(final LockID lockID, final CachedItem item) {
    if (item == null) { throw new NullPointerException(); }
    final int hash = hash(lockID.hashCode());
    final int index = segmentFor(hash);

    this.locks[index].lock();
    try {
      CachedItem head = this.segments[index].remove(lockID);
      head = removeNode(head, item);
      if (head != null) {
        this.segments[index].put(lockID, head);
      }
    } finally {
      this.locks[index].unlock();
    }
  }

  public void flush(final LockID lockID) {
    final int hash = hash(lockID.hashCode());
    final int index = segmentFor(hash);

    this.locks[index].lock();
    try {
      final CachedItem head = this.segments[index].remove(lockID);
      dispose(head);
    } finally {
      this.locks[index].unlock();
    }
  }

  private void dispose(CachedItem head) {
    while (head != null) {
      head.dispose();
      head = head.getNext();
    }
  }

  private CachedItem removeNode(CachedItem head, final CachedItem item) {
    if (head == item) {
      // first item is the item of interest
      head = head.getNext();
      item.setNext(null); // Aid GC
    } else if (head != null) {
      CachedItem current = head;
      CachedItem next;
      while ((next = current.getNext()) != null) {
        if (next == item) {
          current.setNext(next.getNext());
          item.setNext(null); // Aid GC
          break; // hopefully only one occurrence
        }
        current = next;
      }
    }
    return head;
  }

}
