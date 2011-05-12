/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.object.locks.LockID;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LockStore is used for storing all the locks present in the system. Access to any lock can be obtained by checking out
 * a lock and then by checking it in. Locks will be distributed into segments and the location of the lock will be
 * decided on the basis of the a hash function
 * <p>
 * Apart from this getNextLock (for scanning all the locks) has been provided which takes care of the checking out/in of
 * lock.
 */
public class LockStore {
  private static final int                  DEFAULT_SEGMENTS = 32;
  private final HashMap<LockID, ServerLock> segments[];
  private final ReentrantLock[]             guards;
  private final int                         segmentShift;
  private final int                         segmentMask;
  private final LockFactory                 lockFactory;

  public LockStore(LockFactory factory) {
    this(DEFAULT_SEGMENTS, factory);
  }

  public LockStore(int numberOfSegments, LockFactory factory) {
    if (numberOfSegments <= 0) throw new IllegalArgumentException();

    this.lockFactory = factory;
    // Find power-of-two sizes best matching arguments
    int sshift = 0;
    int ssize = 1;
    while (ssize < numberOfSegments) {
      ++sshift;
      ssize <<= 1;
    }
    segmentShift = 32 - sshift;
    segmentMask = ssize - 1;
    numberOfSegments = ssize;

    segments = new HashMap[numberOfSegments];
    guards = new ReentrantLock[numberOfSegments];

    for (int i = 0; i < segments.length; i++) {
      segments[i] = new HashMap();
      guards[i] = new ReentrantLock();
    }
  }

  public ServerLock checkOut(LockID lockID) {
    int index = indexFor(lockID);
    guards[index].lock();
    ServerLock lock = segments[index].get(lockID);
    if (lock == null) {
      lock = lockFactory.createLock(lockID);
      segments[index].put(lockID, lock);
    }
    return lock;
  }

  // Assumption that the lock is already held i.e. checked out
  public ServerLock remove(LockID lockID) {
    int index = indexFor(lockID);
    Assert.assertTrue(guards[index].isHeldByCurrentThread());
    ServerLock lock = segments[index].remove(lockID);
    return lock;
  }

  public void checkIn(ServerLock lock) {
    LockID lockID = lock.getLockID();
    int index = indexFor(lockID);
    if (!guards[index].isHeldByCurrentThread()) { throw new AssertionError("Server Lock " + lock
                                                                           + " was not checked out by the same thread"); }
    guards[index].unlock();
  }

  private final int indexFor(Object o) {
    int hash = hash(o);
    return ((hash >>> segmentShift) & segmentMask);
  }

  /**
   * Currently from CHM
   */
  private static int hash(Object x) {
    int h = x.hashCode();
    h += ~(h << 9);
    h ^= (h >>> 14);
    h += (h << 4);
    h ^= (h >>> 10);
    return h;
  }

  public void clear() {
    for (int i = 0; i < guards.length; i++) {
      guards[i].lock();
      try {
        segments[i].clear();
      } finally {
        guards[i].unlock();
      }
    }
  }

  public LockIterator iterator() {
    return new LockIterator();
  }

  public class LockIterator {
    private Iterator<Entry<LockID, ServerLock>> currentIter;
    private int                                 currentIndex = -1;
    private ServerLock                          oldLock;

    /**
     * This method basically fetches the next lock by checking it out and checks back in the oldLock (that was given
     * last by this method). This method is a replacement for iterator keeping in the check out/in logic. NOTE: If you
     * do not complete the iteration then please check back in the lock. Otherwise it might result in a segment locked
     * forever.
     */
    public ServerLock getNextLock(ServerLock lock) {
      validateOldLock(lock);
      while (currentIter == null || !currentIter.hasNext()) {
        HashMap<LockID, ServerLock> nextSegment = fetchNextSegment();
        if (nextSegment == null) { return null; }
        currentIter = nextSegment.entrySet().iterator();
      }
      Assert.assertNotNull(currentIter);
      oldLock = currentIter.next().getValue();
      return oldLock;
    }

    public void remove() {
      Assert.assertNotNull(currentIter);
      currentIter.remove();
    }

    public void checkIn(ServerLock lock) {
      Assert.assertEquals(oldLock, lock);
      LockStore.this.checkIn(lock);
    }

    private void validateOldLock(ServerLock lock) {
      if (oldLock != null) {
        Assert.assertSame(oldLock, lock);
      } else {
        Assert.assertNull(lock);
      }

    }

    private HashMap<LockID, ServerLock> fetchNextSegment() {
      if (currentIndex >= 0 && currentIndex < segments.length) {
        guards[currentIndex].unlock();
      }
      currentIndex++;
      if (currentIndex >= segments.length) { return null; }

      guards[currentIndex].lock();
      return segments[currentIndex];
    }
  }
}