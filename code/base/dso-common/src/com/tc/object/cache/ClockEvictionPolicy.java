/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import com.tc.text.PrettyPrinter;

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

  public ClockEvictionPolicy(int size) {
    this(size, (int) ((size * 0.1)));
  }

  public ClockEvictionPolicy(int capacity, int evictionSize) {
    this.capacity = capacity;
    this.evictionSize = (evictionSize <= 0 ? 1 : evictionSize);
  }

  public synchronized boolean add(Cacheable obj) {
    if (hand == null) {
      cache.addLast(obj);
    } else {
      cache.addBefore(hand, obj);
    }
    markReferenced(obj);
    return isCacheFull();
  }

  private boolean isCacheFull() {
    if (capacity <= 0 || cache.size() <= capacity) {
      return false;
    } else {
      return true;
    }
  }

  public synchronized Collection getRemovalCandidates(int maxCount) {
    if (capacity > 0) {
      if (!isCacheFull()) return Collections.EMPTY_LIST;
      if (maxCount <= 0 || maxCount > evictionSize) {
        maxCount = evictionSize;
      }
    } else if (maxCount <= 0) {
      // disallow negetative maxCount when capacity is negative
      throw new AssertionError("Please specify maxcount > 0 as capacity is set to : " + capacity + " Max Count = "
                               + maxCount);
    }
    Collection rv = new HashSet();
    int count = Math.min(cache.size(), maxCount);
    while (cache.size() - rv.size() > capacity && count > 0 && moveHand()) {
      rv.add(hand);
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

  public synchronized void remove(Cacheable obj) {
    if (hand != null && obj == hand) {
      hand = (Cacheable) hand.getPrevious();
    }
    cache.remove(obj);
  }

  public void markReferenced(Cacheable obj) {
    obj.markAccessed();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return null;
  }

  private boolean moveHand() {
    boolean found = false;
    while (!found) {
      if (hand == null || hand.getNext() == null) {
        hand = (Cacheable) cache.getFirst();
      } else {
        this.hand = (Cacheable) hand.getNext();
      }
      if (hand.recentlyAccessed()) {
        hand.clearAccessed();
      } else if (hand.canEvict()) {
        found = true;
        break;
      }
      if (hand == save) {
        // logger.info("Cache Evictor : Couldnt find any more ! - cache.size () = " + cache.size());
        break;
      }
      if (save == null) {
        markPosition();
      }
    }
    return found;
  }

  public int getCacheCapacity() {
    return capacity;
  }
}
