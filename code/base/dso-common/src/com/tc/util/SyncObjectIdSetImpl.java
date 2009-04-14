/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.text.PrettyPrinter;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SyncObjectIdSetImpl extends AbstractSet implements SyncObjectIdSet {

  private final Object lock       = new Object();
  private ObjectIDSet  set        = new ObjectIDSet();
  private boolean      isBlocking = false;

  public void startPopulating() {
    synchronized (this.lock) {
      this.isBlocking = true;
    }
  }

  public void stopPopulating(ObjectIDSet fullSet) {
    synchronized (this.lock) {
      ObjectIDSet large = (fullSet.size() > this.set.size()) ? fullSet : this.set;
      ObjectIDSet small = (this.set == large) ? fullSet : this.set;
      large.addAll(small);
      this.set = large;
      this.isBlocking = false;
      this.lock.notifyAll();
    }
  }

  /**
   * A Slightly optimized methods to do add() and size() without grabbing the internal lock twice.
   * 
   * @return size if object was successfully added, else return -1.
   */
  public int addAndGetSize(Object obj) {
    synchronized (this.lock) {
      boolean added = this.set.add(obj);
      if (added) {
        return this.set.size();
      } else {
        return -1;
      }
    }
  }

  @Override
  public boolean add(Object obj) {
    synchronized (this.lock) {
      return this.set.add(obj);
    }
  }

  public void addAll(Set s) {
    synchronized (this.lock) {
      this.set.addAll(s);
    }
  }

  @Override
  public boolean contains(Object o) {
    boolean rv = false;
    synchronized (this.lock) {
      rv = this.set.contains(o);
      if (this.isBlocking && !rv) {
        waitWhileBlocked();
        rv = this.set.contains(o);
      }
    }
    return rv;
  }

  @Override
  public boolean removeAll(Collection ids) {
    boolean rv = false;
    synchronized (this.lock) {
      waitWhileBlocked();
      rv = this.set.removeAll(ids);
    }
    return rv;
  }

  @Override
  public boolean remove(Object o) {
    boolean rv = false;
    synchronized (this.lock) {
      waitWhileBlocked();
      rv = this.set.remove(o);
    }
    return rv;
  }

  @Override
  public Iterator iterator() {
    Iterator rv = null;
    synchronized (this.lock) {
      waitWhileBlocked();
      rv = this.set.iterator();
    }
    return rv;
  }

  @Override
  public int size() {
    int rv = 0;
    synchronized (this.lock) {
      waitWhileBlocked();
      rv = this.set.size();
    }
    return rv;
  }

  public ObjectIDSet snapshot() {
    synchronized (this.lock) {
      waitWhileBlocked();
      return new ObjectIDSet(this.set);
    }
  }

  private void waitWhileBlocked() {
    try {
      while (this.isBlocking) {
        this.lock.wait();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName());
    synchronized (this.lock) {
      out.indent().print("blocking : ").print(Boolean.valueOf(this.isBlocking));
      out.indent().print("id set   : ").visit(this.set);
    }
    return out;
  }

}
