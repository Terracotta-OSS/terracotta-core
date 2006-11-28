/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.text.PrettyPrinter;

import gnu.trove.TLinkable;
import gnu.trove.TLinkedList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * This Cache policy tries to evict objects from cache using the access count. The Least Frequenctly used objects are
 * chucked out.
 */
public class LFUEvictionPolicy implements EvictionPolicy {

  private static final TCLogger logger = TCLogging.getLogger(LFUEvictionPolicy.class);
  private final int             capacity;
  private final int             evictionSize;
  private final TLinkedList     cache  = new TLinkedList();
  // This is a marker in the linked list where the old (non-new) objects starts.
  private TLinkable             mark;

  public LFUEvictionPolicy(int capacity) {
    this(capacity, capacity / 10);
  }

  public LFUEvictionPolicy(int capacity, int evictionSize) {
    if (logger.isDebugEnabled()) {
      logger.debug("new " + getClass().getName() + "(" + capacity + ")");
    }
    this.capacity = capacity;
    this.evictionSize = (evictionSize <= 0 ? 1 : evictionSize);
  }

  public synchronized boolean add(Cacheable obj) {
    cache.addBefore(mark, obj);
    return isCacheFull();
  }

  private boolean isCacheFull() {
    return (capacity > 0 && cache.size() > capacity);
  }

  public int getCacheCapacity() {
    return capacity;
  }

  // public synchronized Collection getRemovalCandidates(int maxCount) {
  // if (capacity > 0) {
  // if (!isCacheFull()) { return Collections.EMPTY_LIST; }
  // if (maxCount <= 0 || maxCount > evictionSize) {
  // maxCount = evictionSize;
  // }
  // } else if (maxCount <= 0) {
  // // disallow negetative maxCount when capacity is negative
  // throw new AssertionError("Please specify maxcount > 0 as capacity is set to : " + capacity + " Max Count = "
  // + maxCount);
  // }
  //
  // Collection rv = new HashSet();
  // int count = Math.min(cache.size(), maxCount);
  //
  // // TODO::FIXME::If for some reason the cachemanager is not able to evict some of the removal candidates, then it is
  // // given out again !!
  // Cacheable c = (Cacheable) mark;
  // if (c == null) {
  // c = (Cacheable) cache.getFirst();
  // }
  // while (cache.size() - rv.size() > capacity && count > 0 && c != null) {
  // // moveToTail(c);
  // if (c.canEvict()) {
  // rv.add(c);
  // count--;
  // }
  // c = (Cacheable) c.getNext();
  // }
  // mark = (Cacheable) cache.getFirst();
  // return rv;
  // }
  //
  // public synchronized void markReferenced(Cacheable obj) {
  // long start = System.currentTimeMillis();
  // obj.markAccessed();
  // int accessCount = obj.accessCount();
  // Cacheable next = (Cacheable) obj.getNext();
  // if (next == null || next.accessCount() >= accessCount) { return; }
  // while (next != null && next.accessCount() < accessCount) {
  // next = (Cacheable) next.getNext();
  // }
  // if (mark == null || mark == next) {
  // mark = obj;
  // } else if (mark == obj) {
  // mark = obj.getNext();
  // }
  // cache.remove(obj);
  // cache.addBefore(next, obj);
  // long end = System.currentTimeMillis();
  // if(end-start > 500) {
  // logger.info("BAD : Mark Reference took " + (end-start) +" ms");
  // }
  // }

  public synchronized void markReferenced(Cacheable obj) {
    obj.markAccessed();
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
    Map accessCountSummary = new TreeMap(); // This is sorted map

    Cacheable c = (Cacheable) mark;
    if (c == null) {
      c = (Cacheable) cache.getFirst();
    }
    Cacheable save = c;
    mark = (Cacheable) cache.getFirst();

    // Step 1: Remove elements which were never accessed and at the same time collect stats
    while (cache.size() - rv.size() > capacity && count > 0 && c != null) {
      Cacheable next = (Cacheable) c.getNext();
      int accessed = c.accessCount();
      if (accessed == 0) {
        if (c.canEvict()) {
          rv.add(c);
          if (mark != c) {
            cache.remove(c);
            cache.addBefore(mark, c);
          }
          count--;
        }
      } else {
        incrementAccessCountFor(accessCountSummary, accessed);

      }
      c = next;
    }
    if (cache.size() - rv.size() <= capacity || count <= 0) {
      // we already got what is needed
      return rv;
    }

    // Step 2: Use the summary that was built earlier to decide the accessCountCutOff
    int accessCountCutOff = 0;
    int remaining = count;
    for (Iterator i = accessCountSummary.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      accessCountCutOff = ((Integer) e.getKey()).intValue();
      int occurance = ((Integer) e.getValue()).intValue();
      remaining -= occurance;
      if (remaining <= 0) {
        break;
      }
    }
    c = save;
    while (cache.size() - rv.size() > capacity && count > 0 && c != null) {
      Cacheable next = (Cacheable) c.getNext();
      int accessed = c.accessCount();
      if (accessed <= accessCountCutOff) {
        if (c.canEvict()) {
          rv.add(c);
          if (mark != c) {
            cache.remove(c);
            cache.addBefore(mark, c);
          }
          count--;
        }
      }
      c = next;
    }
    return rv;
  }

  private void incrementAccessCountFor(Map accessCountSummary, int ac) {
    Integer key = new Integer(ac);
    Integer count = (Integer) accessCountSummary.get(key);
    if (count == null) {
      accessCountSummary.put(key, new Integer(1));
    } else {
      accessCountSummary.put(key, new Integer(count.intValue() + 1));
    }
  }

  private boolean contains(Cacheable obj) {
    // XXX: This is here to get around bogus implementation of TLinkedList.contains(Object)
    return obj != null && (obj.getNext() != null || obj.getPrevious() != null);
  }

  public synchronized void remove(Cacheable obj) {
    if (contains(obj)) {
      if (mark == obj) {
        mark = obj.getNext();
      }
      cache.remove(obj);
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out.println(getClass().getName());
    out = out.duplicateAndIndent();
    out.indent().println("max size: " + capacity).indent().print("cache: ").visit(cache).println();
    return rv;
  }

}
