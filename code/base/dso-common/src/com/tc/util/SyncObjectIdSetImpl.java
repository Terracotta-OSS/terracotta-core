/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SyncObjectIdSetImpl extends AbstractSet implements SyncObjectIdSet {

  private final Object lock       = new Object();
  private ObjectIDSet set        = new ObjectIDSet();
  private boolean      isBlocking = false;

  public void startPopulating() {
    synchronized (lock) {
      isBlocking = true;
    }
  }

  public void stopPopulating(ObjectIDSet fullSet) {
    synchronized (lock) {
      ObjectIDSet large = (fullSet.size() > set.size()) ? fullSet : set;
      ObjectIDSet small = (set == large) ? fullSet : set;
      large.addAll(small);
      set = large;
      isBlocking = false;
      lock.notifyAll();
    }
  }

  public boolean add(Object obj) {
    synchronized (lock) {
      return set.add(obj);
    }
  }

  public void addAll(Set s) {
    synchronized (lock) {
      set.addAll(s);
    }
  }

  public boolean contains(Object o) {
    boolean rv = false;
    synchronized (lock) {
      rv = set.contains(o);
      if (isBlocking && !rv) {
        waitWhileBlocked();
        rv = set.contains(o);
      }
    }
    return rv;
  }

  public boolean removeAll(Collection ids) {
    boolean rv = false;
    synchronized (lock) {
      waitWhileBlocked();
      rv = set.removeAll(ids);
    }
    return rv;
  }

  public boolean remove(Object o) {
    boolean rv = false;
    synchronized (lock) {
      waitWhileBlocked();
      rv = set.remove(o);
    }
    return rv;
  }

  public Iterator iterator() {
    Iterator rv = null;
    synchronized (lock) {
      waitWhileBlocked();
      rv = set.iterator();
    }
    return rv;
  }

  public int size() {
    int rv = 0;
    synchronized (lock) {
      waitWhileBlocked();
      rv = set.size();
    }
    return rv;
  }

  public ObjectIDSet snapshot() {
    synchronized (lock) {
      waitWhileBlocked();
      return new ObjectIDSet(set);
    }
  }

  private void waitWhileBlocked() {
    try {
      while (isBlocking) {
        lock.wait();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
