/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.text.PrettyPrinter;

import gnu.trove.TLinkedList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Whimpy implementation of an LRU cache
 */
public class LRUEvictionPolicy implements EvictionPolicy {
  private static final TCLogger logger = TCLogging.getLogger(LRUEvictionPolicy.class);
  private final TLinkedList     cache  = new TLinkedList();
  private final int             capacity;
  private final int             evictionSize;

  public LRUEvictionPolicy(int capacity) {
    this(capacity, capacity / 10);
  }

  public LRUEvictionPolicy(int capacity, int evictionSize) {
    if (logger.isDebugEnabled()) {
      logger.debug("new " + getClass().getName() + "(" + capacity + ")");
    }
    this.capacity = capacity;
    this.evictionSize = (evictionSize <= 0 ? 1 : evictionSize);
  }

  public synchronized boolean add(Cacheable obj) {
    // Assert.eval(!contains(obj));
    if (logger.isDebugEnabled()) {
      logger.debug("Adding: " + obj);
    }
    cache.addLast(obj);

    return isCacheFull();
  }

  private boolean isCacheFull() {
    return (capacity > 0 && cache.size() > capacity);
  }

  public synchronized Collection getRemovalCandidates(int maxCount) {
    if (capacity > 0) {
      if (!isCacheFull()) { return Collections.EMPTY_LIST; }
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
    Cacheable c = (Cacheable) cache.getFirst();
    Object save = c;
    while (cache.size() - rv.size() > capacity && count > 0) {
      moveToTail(c);
      if (c.canEvict()) {
        rv.add(c);
        count--;
      }
      c = (Cacheable) cache.getFirst();
      if (save == c) break;
    }
    return rv;
  }

  public synchronized void remove(Cacheable obj) {
    if (logger.isDebugEnabled()) {
      logger.debug("Removing: " + obj);
    }
    if (contains(obj)) cache.remove(obj);
  }

  private boolean contains(Cacheable obj) {
    // XXX: This is here to get around bogus implementation of TLinkedList.contains(Object)
    return obj != null && (obj.getNext() != null || obj.getPrevious() != null);
  }

  public synchronized void markReferenced(Cacheable obj) {
    moveToTail(obj);
  }

  private synchronized void moveToTail(Cacheable obj) {
    if (contains(obj)) {
      cache.remove(obj);
      cache.addLast(obj);
    }
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out.println(getClass().getName());
    out = out.duplicateAndIndent();
    out.indent().println("max size: " + capacity).indent().print("cache: ").visit(cache).println();
    return rv;
  }

  public int getCacheCapacity() {
    return capacity;
  }

}
