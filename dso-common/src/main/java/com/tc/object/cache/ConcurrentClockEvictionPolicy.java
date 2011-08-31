/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.ObjectID;
import com.tc.text.PrettyPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentClockEvictionPolicy implements EvictionPolicy {

  private final ConcurrentMap<ObjectID, Cacheable> map;
  private Iterator<Entry<ObjectID, Cacheable>>     clock;

  public ConcurrentClockEvictionPolicy() {
    this(new ConcurrentHashMap<ObjectID, Cacheable>(10240, 0.75f, 256));
  }

  public ConcurrentClockEvictionPolicy(final ConcurrentHashMap<ObjectID, Cacheable> map) {
    this.map = map;
    this.clock = newIterator();
  }

  private Iterator newIterator() {
    return this.map.entrySet().iterator();
  }

  public boolean add(final Cacheable obj) {
    this.map.put(obj.getObjectID(), obj);
    markReferenced(obj);
    return false;
  }

  public int getCacheCapacity() {
    return -1;
  }

  public Collection getRemovalCandidates(final int maxCount) {
    final ArrayList list = new ArrayList(maxCount);
    int count = this.map.size();
    while (count-- > 0 && list.size() < maxCount) {
      final Entry<ObjectID, Cacheable> e = moveHand();
      if (e == null) {
        break;
      }
      final Cacheable c = e.getValue();
      if (c == null) {
        continue;
      } else if (c.recentlyAccessed()) {
        c.clearAccessed();
      } else if (c.canEvict()) {
        list.add(c);
      }
    }
    return list;
  }

  private Entry<ObjectID, Cacheable> moveHand() {
    if (this.clock.hasNext()) { return this.clock.next(); }
    this.clock = newIterator();
    if (this.clock.hasNext()) { return this.clock.next(); }
    return null;
  }

  public void markReferenced(final Cacheable obj) {
    obj.markAccessed();
  }

  public void remove(final Cacheable obj) {
    this.map.remove(obj.getObjectID());
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    return null;
  }

}
