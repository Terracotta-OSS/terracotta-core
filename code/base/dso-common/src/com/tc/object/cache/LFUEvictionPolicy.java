/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import gnu.trove.TLinkedList;
import gnu.trove.TObjectLongHashMap;

import java.util.ArrayList;
import java.util.Arrays;
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

  private static final TCLogger    logger         = TCLogging.getLogger(LFUEvictionPolicy.class);

  private static final LFUConfig   DEFAULT_CONFIG = new LFUConfig() {

                                                    public float getAgingFactor() {
                                                      // DISABLED
                                                      return 1;
                                                    }

                                                    public int getRecentlyAccessedIgnorePercentage() {
                                                      return 20;
                                                    }

                                                    public boolean isDebugEnabled() {
                                                      return false;
                                                    }

                                                  };

  private final int                capacity;
  private final int                evictionSize;
  private final TLinkedList        cache          = new TLinkedList();
  private final LFUConfig          config;

  private final TObjectLongHashMap smap           = new TObjectLongHashMap();

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
    if (this.config.isDebugEnabled()) {
      this.smap.put(obj.getObjectID(), System.currentTimeMillis());
    }
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

  public Collection getRemovalCandidates(final int maxCount) {
    final Collection candidates = getRemovalCandidatesInternal(maxCount);
    if (this.config.isDebugEnabled()) {
      reportTime("Cache", this.cache.subList(0, this.cache.size()));
      reportTime("Eviction candidates", candidates);
    }
    return candidates;
  }

  private void reportTime(final String name, final Collection cacheables) {
    final long now = System.currentTimeMillis();
    final long times[] = new long[cacheables.size()];
    int j = 0;
    long avg = 0;
    synchronized (this) {
      for (final Iterator i = cacheables.iterator(); i.hasNext();) {
        final Cacheable c = (Cacheable) i.next();
        final long diff = now - this.smap.get(c.getObjectID());
        times[j++] = diff;
        avg += diff;
      }
    }
    avg = avg / times.length;
    // Stupid but whatever
    Arrays.sort(times);
    final StringBuffer sb = new StringBuffer(name);
    sb.append(" : size = ").append(cacheables.size()).append(" Avg = ").append(avg);
    sb.append(" Min = ").append(times[0]);
    sb.append(" Max = ").append(times[times.length - 1]);
    sb.append("\n\n");
    final int n10 = times.length / 10;
    for (int i = 1; i < 10; i++) {
      sb.append("\t").append(i * 10).append(" % = ").append(times[n10 * i]).append("\n");
    }
    sb.append("\n\n");
    logger.info(sb.toString());
  }

  private Collection getRemovalCandidatesInternal(int maxCount) {

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
        // disallow negetative maxCount when capacity is negative
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
          accessCounts.add(new Integer(accessed));

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
      accessCountSummary.put(key, new Integer(1));
    } else {
      accessCountSummary.put(key, new Integer(count.intValue() + 1));
    }
  }

  private boolean contains(final Cacheable obj) {
    // XXX: This is here to get around bogus implementation of TLinkedList.contains(Object)
    return obj != null && (obj.getNext() != null || obj.getPrevious() != null);
  }

  public synchronized void remove(final Cacheable obj) {
    if (contains(obj)) {
      this.cache.remove(obj);
      if (this.config.isDebugEnabled()) {
        this.smap.remove(obj.getObjectID());
      }
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
