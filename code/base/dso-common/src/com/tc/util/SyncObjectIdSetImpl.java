/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SyncObjectIdSetImpl extends AbstractSet implements SyncObjectIdSet {

  private final Object lock       = new Object();
  private ObjectIDSet2 set        = new ObjectIDSet2();
  private boolean      isBlocking = false;

  public void startPopulating() {
    synchronized (lock) {
      isBlocking = true;
    }
  }

  public void stopPopulating(ObjectIDSet2 fullSet) {
    synchronized (lock) {
      isBlocking = false;
      ObjectIDSet2 large = (fullSet.size() > set.size()) ? fullSet : set;
      ObjectIDSet2 small = (set == large) ? fullSet : set;
      large.addAll(small);
      set = large;
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
    synchronized(lock) {
      rv = set.contains(o);
      if (isBlocking && !rv) {
        waitOn(lock);
        rv = set.contains(o);
      }
    }
    return rv;
  }

  public boolean removeAll(Collection ids) {
    boolean rv = false;
    synchronized (lock) {
      if (isBlocking) waitOn(lock);
      rv = set.removeAll(ids);
    }
    return rv;
  }

  public boolean remove(Object o) {
    boolean rv = false;
    synchronized (lock) {
      if(isBlocking) waitOn(lock);
      rv = set.remove(o);
    }
    return rv;
  }

  public Iterator iterator() {
    Iterator rv = null;
    synchronized (lock) {
      if (isBlocking) waitOn(lock);
      rv = set.iterator();
    }
    return rv;
  }

  public int size() {
    int rv = 0;
    synchronized (lock) {
      if (isBlocking) waitOn(lock);
      rv = set.size();
    }
    return rv;
  }

  public SyncObjectIdSet snapshot() {
    SyncObjectIdSetImpl rv = new SyncObjectIdSetImpl();
    synchronized (lock) {
      if (isBlocking) waitOn(lock);
      rv.addAll(set);
    }
    return rv;
  }

  private static void waitOn(Object l) {
    try {
      l.wait();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
