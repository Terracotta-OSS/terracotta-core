/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import gnu.trove.TLinkedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This Cache policy tries to evict objects from cache using the access count. The Least Frequenctly used objects are
 * chucked out.
 */
public class LFUEvictionPolicy implements EvictionPolicy {

  private static final TCLogger  logger         = TCLogging.getLogger(LFUEvictionPolicy.class);

  private static final LFUConfig DEFAULT_CONFIG = new LFUConfig() {

                                                  public float getAgingFactor() {
                                                    // DISABLED
                                                    return 1;
                                                  }

                                                  public int getRecentlyAccessedIgnorePercentage() {
                                                    return 20;
                                                  }
                                                };

  private final int              capacity;
  private final int              evictionSize;
  private final TLinkedList      cache          = new TLinkedList();
  private final LFUConfig        config;

  public LFUEvictionPolicy(final int capacity) {
    this(capacity, capacity / 10, DEFAULT_CONFIG);
  }

  public LFUEvictionPolicy(final int capacity, final LFUConfig config) {
    this(capacity, capacity / 10, config);
  }

  public LFUEvictionPolicy(final int capacity, final int evictionSize) {
    this(capacity, evictionSize, DEFAULT_CONFIG);
  }

  public LFUEvictionPolicy(final int capacity, final int evictionSize, final LFUConfig config) {
    if (logger.isDebugEnabled()) {
      logger.debug("new " + getClass().getName() + "(" + capacity + ")");
    }
    this.capacity = capacity;
    this.evictionSize = (evictionSize <= 0 ? 1 : evictionSize);
    this.config = config;
  }

  public synchronized boolean add(final Cacheable obj) {
    Assert.assertTrue(obj.getNext() == null && obj.getPrevious() == null);
    this.cache.addLast(obj);
    return isCacheFull();
  }

  private boolean isCacheFull() {
    return (this.capacity > 0 && this.cache.size() > this.capacity);
  }

  public int getCacheCapacity() {
    return this.capacity;
  }

  public synchronized void markReferenced(final Cacheable obj) {
    obj.markAccessed();
    moveToTail(obj);
  }

  private void moveToTail(final Cacheable obj) {
    if (contains(obj)) {
      this.cache.remove(obj);
      this.cache.addLast(obj);
    }
  }

  public Collection getRemovalCandidates(int maxCount) {

    final long start = System.currentTimeMillis();
    final Collection rv = new HashSet();
    int count = 0;
    ArrayList accessCounts;
    Cacheable stop, c;
    synchronized (this) {
      if (this.capacity > 0) {
        if (!isCacheFull()) { return Collections.EMPTY_LIST; }
        if (maxCount <= 0 || maxCount > this.evictionSize) {
          maxCount = this.evictionSize;
        }
      } else if (maxCount <= 0) {
        // disallow negative maxCount when capacity is negative
        throw new AssertionError("Please specify maxcount > 0 as capacity is set to : " + this.capacity
                                 + " Max Count = " + maxCount);
      }

      count = Math.min(this.cache.size(), maxCount);
      accessCounts = new ArrayList(this.cache.size());

      c = (Cacheable) this.cache.getFirst();
      stop = getStopPoint();
      final int agingFactor = (int) this.config.getAgingFactor();
      // Step 1: Remove elements which were never accessed and at the same time collect stats
      while (this.cache.size() - rv.size() > this.capacity && count > 0 && c != null && c != stop) {
        final Cacheable next = (Cacheable) c.getNext();
        final int accessed = c.accessCount(agingFactor);
        if (accessed == 0) {
          if (c.canEvict()) {
            rv.add(c);
            this.cache.remove(c);
            this.cache.addLast(c);
            count--;
          }
        } else {
          // incrementAccessCountFor(accessCountSummary, accessed);
          accessCounts.add(Integer.valueOf(accessed));

        }
        c = next;
      }
      while (c != null && c != stop) {
        c.accessCount(agingFactor);
        c = (Cacheable) c.getNext();
      }
      if (this.cache.size() - rv.size() <= this.capacity || count <= 0) {
        // we already got what is needed
        log_time_taken(start);
        return rv;
      }
    }

    // Step 2: Do the sorting ... This can be optimized since we dont need it to be sorted.
    Map accessCountSummary = new TreeMap(); // This is sorted map
    for (final Iterator i = accessCounts.iterator(); i.hasNext();) {
      final Integer ac = (Integer) i.next();
      incrementAccessCountFor(accessCountSummary, ac);
    }

    // release memory when done
    accessCounts = null;

    // Step 3: Use the summary that was built earlier to decide the accessCountCutOff
    int accessCountCutOff = 0;
    int remaining = count;
    for (final Iterator i = accessCountSummary.entrySet().iterator(); i.hasNext();) {
      final Entry e = (Entry) i.next();
      accessCountCutOff = ((Integer) e.getKey()).intValue();
      final int occurance = ((Integer) e.getValue()).intValue();
      remaining -= occurance;
      if (remaining <= 0) {
        break;
      }
    }

    // release memory when done
    accessCountSummary = null;

    // Step 4 : Use the calculated accessCountCutOff to get the rigth candidates under the lock. Since we release teh
    // lock,
    // we have to be fault tolerant
    synchronized (this) {
      c = (Cacheable) this.cache.getFirst();
      while (this.cache.size() - rv.size() > this.capacity && count > 0 && c != null && c != stop) {
        final Cacheable next = (Cacheable) c.getNext();
        final int accessed = c.accessCount(1);
        if (accessed <= accessCountCutOff) {
          if (c.canEvict()) {
            rv.add(c);
            this.cache.remove(c);
            this.cache.addLast(c);
            count--;
          }
        }
        c = next;
      }
      log_time_taken(start);
      return rv;
    }
  }

  private Cacheable getStopPoint() {
    // The last LRU_IN_MEMORY_PERCENTAGE of element are not processed to be fair with new objects/recently accessed
    // objects
    Cacheable stop = (Cacheable) this.cache.getLast();
    int ignore = (int) (this.cache.size() * this.config.getRecentlyAccessedIgnorePercentage() / 100.0); // force
                                                                                                        // floating
                                                                                                        // point
    // arithemetic
    while (ignore-- > 0) {
      stop = (Cacheable) stop.getPrevious();
    }
    return stop;
  }

  private void log_time_taken(final long start) {
    final long taken = System.currentTimeMillis() - start;
    if (taken > 1000) {
      logger.info("Time taken to compute removal candidates : " + taken + " ms");
    }
  }

  private void incrementAccessCountFor(final Map accessCountSummary, final Integer key) {
    final Integer count = (Integer) accessCountSummary.get(key);
    if (count == null) {
      accessCountSummary.put(key, Integer.valueOf(1));
    } else {
      accessCountSummary.put(key, Integer.valueOf(count.intValue() + 1));
    }
  }

  private boolean contains(final Cacheable obj) {
    // XXX: This is here to get around bogus implementation of TLinkedList.contains(Object)
    return obj != null && (obj.getNext() != null || obj.getPrevious() != null);
  }

  public synchronized void remove(final Cacheable obj) {
    if (contains(obj)) {
      this.cache.remove(obj);
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    final PrettyPrinter rv = out;
    out.println(getClass().getName());
    out = out.duplicateAndIndent();
    out.indent().println("max size: " + this.capacity).indent().print("cache: ").visit(this.cache).println();
    return rv;
  }

}
