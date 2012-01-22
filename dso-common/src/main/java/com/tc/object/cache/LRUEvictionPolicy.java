/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

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

  public LRUEvictionPolicy(final int capacity) {
    this(capacity, capacity / 10);
  }

  public LRUEvictionPolicy(final int capacity, final int evictionSize) {
    if (logger.isDebugEnabled()) {
      logger.debug("new " + getClass().getName() + "(" + capacity + ")");
    }
    this.capacity = capacity;
    this.evictionSize = (evictionSize <= 0 ? 1 : evictionSize);
  }

  public synchronized boolean add(final Cacheable obj) {
    // Assert.eval(!contains(obj));
    if (logger.isDebugEnabled()) {
      logger.debug("Adding: " + obj);
    }
    Assert.assertTrue(obj.getNext() == null && obj.getPrevious() == null);
    this.cache.addLast(obj);

    return isCacheFull();
  }

  private boolean isCacheFull() {
    return (this.capacity > 0 && this.cache.size() > this.capacity);
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
    Cacheable c = (Cacheable) this.cache.getFirst();
    final Object save = c;
    while (this.cache.size() - rv.size() > this.capacity && count > 0) {
      moveToTail(c);
      if (c.canEvict()) {
        rv.add(c);
        count--;
      }
      c = (Cacheable) this.cache.getFirst();
      if (save == c) {
        break;
      }
    }
    return rv;
  }

  public synchronized void remove(final Cacheable obj) {
    if (logger.isDebugEnabled()) {
      logger.debug("Removing: " + obj);
    }
    if (contains(obj)) {
      this.cache.remove(obj);
    }
  }

  private boolean contains(final Cacheable obj) {
    // XXX: This is here to get around bogus implementation of TLinkedList.contains(Object)
    return obj != null && (obj.getNext() != null || obj.getPrevious() != null);
  }

  public synchronized void markReferenced(final Cacheable obj) {
    moveToTail(obj);
  }

  private synchronized void moveToTail(final Cacheable obj) {
    if (contains(obj)) {
      this.cache.remove(obj);
      this.cache.addLast(obj);
    }
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    final PrettyPrinter rv = out;
    out.println(getClass().getName());
    out = out.duplicateAndIndent();
    out.indent().println("max size: " + this.capacity).indent().print("cache: ").visit(this.cache).println();
    return rv;
  }

  public int getCacheCapacity() {
    return this.capacity;
  }

}
