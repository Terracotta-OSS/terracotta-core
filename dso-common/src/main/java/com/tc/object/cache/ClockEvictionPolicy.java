/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import gnu.trove.TLinkedList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class ClockEvictionPolicy implements EvictionPolicy {

  // private static final TCLogger logger = new LossyTCLogger(TCLogging.getLogger(ClockEvictionPolicy.class), 1000);

  private final TLinkedList cache = new TLinkedList();
  private final int         capacity;
  private Cacheable         hand  = null;
  private final int         evictionSize;
  private Cacheable         save;

  public ClockEvictionPolicy(final int size) {
    this(size, (int) ((size * 0.1)));
  }

  public ClockEvictionPolicy(final int capacity, final int evictionSize) {
    this.capacity = capacity;
    this.evictionSize = (evictionSize <= 0 ? 1 : evictionSize);
  }

  public synchronized boolean add(final Cacheable obj) {
    Assert.assertTrue(obj.getNext() == null && obj.getPrevious() == null);
    if (this.hand == null) {
      this.cache.addLast(obj);
    } else {
      this.cache.addBefore(this.hand, obj);
    }
    markReferenced(obj);
    return isCacheFull();
  }

  private boolean isCacheFull() {
    if (this.capacity <= 0 || this.cache.size() <= this.capacity) {
      return false;
    } else {
      return true;
    }
  }

  public synchronized Collection getRemovalCandidates(int maxCount) {
    if (this.capacity > 0) {
      if (!isCacheFull()) { return Collections.EMPTY_LIST; }
      if (maxCount <= 0 || maxCount > this.evictionSize) {
        maxCount = this.evictionSize;
      }
    } else if (maxCount <= 0) {
      // disallow negetative maxCount when capacity is negative
      throw new AssertionError("Please specify maxcount > 0 as capacity is set to : " + this.capacity + " Max Count = "
                               + maxCount);
    }
    final Collection rv = new HashSet();
    int count = Math.min(this.cache.size(), maxCount);
    while (this.cache.size() - rv.size() > this.capacity && count > 0 && moveHand()) {
      rv.add(this.hand);
      count--;
    }
    erasePosition();
    return rv;
  }

  private void erasePosition() {
    this.save = null;
  }

  private void markPosition() {
    this.save = this.hand;
  }

  public synchronized void remove(final Cacheable obj) {
    if (this.hand != null && obj == this.hand) {
      this.hand = (Cacheable) this.hand.getPrevious();
    }
    this.cache.remove(obj);
  }

  public void markReferenced(final Cacheable obj) {
    obj.markAccessed();
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    return null;
  }

  private boolean moveHand() {
    boolean found = false;
    while (!found) {
      if (this.hand == null || this.hand.getNext() == null) {
        this.hand = (Cacheable) this.cache.getFirst();
      } else {
        this.hand = (Cacheable) this.hand.getNext();
      }
      if (this.hand.recentlyAccessed()) {
        this.hand.clearAccessed();
      } else if (this.hand.canEvict()) {
        found = true;
        break;
      }
      if (this.hand == this.save) {
        // logger.info("Cache Evictor : Couldnt find any more ! - cache.size () = " + cache.size());
        break;
      }
      if (this.save == null) {
        markPosition();
      }
    }
    return found;
  }

  public int getCacheCapacity() {
    return this.capacity;
  }
}
