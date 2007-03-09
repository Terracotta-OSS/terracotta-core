/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public class SyncObjectIdSet extends AbstractSet {

  private final ObjectIDSet2                           set          = new ObjectIDSet2();
  private final ReentrantWriterPreferenceReadWriteLock rwLock       = new ReentrantWriterPreferenceReadWriteLock();
  private final ReentrantWriterPreferenceReadWriteLock populateLock = new ReentrantWriterPreferenceReadWriteLock();
  private final SynchronizedBoolean                    isPopulating = new SynchronizedBoolean(false);

  public boolean add(Object obj) {
    boolean rv = false;
    try {
      rwLock.writeLock().acquire();
      rv = set.add(obj);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      rwLock.writeLock().release();
    }
    return rv;
  }

  public void stopPopulating() {
    isPopulating.set(false);
    populateLock.writeLock().release();
  }

  public void startPopulating() {
    try {
      populateLock.writeLock().acquire();
      isPopulating.set(true);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public boolean contains(Object o) {
    if (isPopulating.get() && !nonblockingContains(o))
      return blockingContains(o);
    else 
      return nonblockingContains(o);
  }
  
  private boolean nonblockingContains(Object id) {
    boolean rv = false;
    try {
      rwLock.readLock().acquire();
      rv = set.contains(id);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      rwLock.readLock().release();
    }
    return rv;
  }

  private boolean blockingContains(Object id) {
    boolean rv = false;
    try {
      populateLock.readLock().acquire();
      rv = nonblockingContains(id);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      populateLock.readLock().release();
    }
    return rv;
  }

  public boolean removeAll(Collection ids) {
    boolean rv = false;
    try {
      rwLock.writeLock().acquire();
      rv = set.removeAll(ids);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      rwLock.writeLock().release();
    }
    return rv;
  }
  
  public boolean remove(Object o) {
    boolean rv = false;
    try {
      rwLock.writeLock().acquire();
      rv = set.remove(o);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      rwLock.writeLock().release();
    }
    return rv;
  }

  public Iterator iterator() {
    Iterator rv = null;
    try {
      rwLock.readLock().acquire();
      rv = set.iterator();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      rwLock.readLock().release();
    }
    return rv;
  }

  public int size() {
    int rv = 0;
    try {
      rwLock.readLock().acquire();
      rv = set.size();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      rwLock.readLock().release();
    }
    return rv;
  }

  public SyncObjectIdSet snapshot() {
    SyncObjectIdSet rv = new SyncObjectIdSet();
    try {
      rwLock.readLock().acquire();
      rv.addAll(this);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      rwLock.readLock().release();
    }
    return rv;
  }
}
