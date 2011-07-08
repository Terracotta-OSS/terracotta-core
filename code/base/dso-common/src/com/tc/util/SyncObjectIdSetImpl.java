/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
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

  public void stopPopulating(final ObjectIDSet fullSet) {
    synchronized (this.lock) {
      final ObjectIDSet large = (fullSet.size() > this.set.size()) ? fullSet : this.set;
      final ObjectIDSet small = (this.set == large) ? fullSet : this.set;
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
  public int addAndGetSize(final ObjectID obj) {
    synchronized (this.lock) {
      final boolean added = this.set.add(obj);
      if (added) {
        return this.set.size();
      } else {
        return -1;
      }
    }
  }

  @Override
  public boolean add(final Object obj) {
    synchronized (this.lock) {
      return this.set.add((ObjectID) obj);
    }
  }

  public void addAll(final Set s) {
    synchronized (this.lock) {
      this.set.addAll(s);
    }
  }

  @Override
  public boolean contains(final Object o) {
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
  public boolean removeAll(final Collection ids) {
    boolean rv = false;
    synchronized (this.lock) {
      waitWhileBlocked();
      rv = this.set.removeAll(ids);
    }
    return rv;
  }

  @Override
  public boolean remove(final Object o) {
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
    boolean interrupted = false;
    try {
      while (this.isBlocking) {
        try {
          this.lock.wait();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.println(getClass().getName());
    synchronized (this.lock) {
      out.indent().print("blocking : ").print(Boolean.valueOf(this.isBlocking));
      out.indent().print("id set   : ").visit(this.set);
    }
    return out;
  }

}
