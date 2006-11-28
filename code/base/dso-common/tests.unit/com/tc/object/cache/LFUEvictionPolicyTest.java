/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.ObjectID;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;

public class LFUEvictionPolicyTest extends LRUEvictionPolicyTest {

  public void tests() throws Exception {
    int capacity = 50;
    int maxObjects = capacity * 2;
    EvictionPolicy slc = createNewCache(capacity);

    LinkedHashMap cacheables = new LinkedHashMap();
    Random r = new Random();
    for (int i = 1; i < 20000; i++) {
      ObjectID id = new ObjectID(r.nextInt(maxObjects));
      TestCacheable tc = (TestCacheable) cacheables.get(id);
      if (tc == null) {
        tc = new TestCacheable(id);
        cacheables.put(id, tc);
        boolean full = slc.add(tc);
        if (full && cacheables.size() <= capacity) {
          throw new AssertionError("Cache is full when capacity = " + capacity + "and size () = " + cacheables.size());
        } else if (!full && cacheables.size() > capacity) {
          // Formatter
          throw new AssertionError("Cache is Not full when capacity = " + capacity + "and size () = "
                                   + cacheables.size());
        }
      } else {
        int accessed = tc.accessCount();
        slc.markReferenced(tc);
        if (++accessed != tc.accessCount()) { throw new AssertionError("Accessed Count Mismatch : " + tc.accessCount()
                                                                       + " Expected : " + accessed); }
      }
    }

    while (cacheables.size() > capacity) {
      Collection rc = slc.getRemovalCandidates(10);
      int maxAccessed = -1;
      for (Iterator i = rc.iterator(); i.hasNext();) {
        TestCacheable tc = (TestCacheable) i.next();
        if (maxAccessed < tc.accessCount()) {
          maxAccessed = tc.accessCount();
        }
        slc.remove(tc);
        assertTrue(cacheables.remove(tc.getObjectID()) == tc);
      }
      System.err.println("Max Accessed is : " + maxAccessed);
      int errorThreshold = 0;
      for (Iterator i = cacheables.values().iterator(); i.hasNext();) {
        TestCacheable tc = (TestCacheable) i.next();
        assertFalse(rc.contains(tc));
        if (tc.accessCount() < maxAccessed) {
          System.err.println("WARNING : maxAccessed : " + maxAccessed + " tc.accessCount() " + tc.accessCount());
          // throw new AssertionError("Got an Object that is accessed more that one available in cache");
          errorThreshold++;
        }
      }
      if(errorThreshold >= rc.size()) {
        throw new AssertionError("Beyond Error Threshold : " + errorThreshold);
      }
    }
  }

  public EvictionPolicy createNewCache(int cacheSize) {
    return new LFUEvictionPolicy(cacheSize);
  }

}
