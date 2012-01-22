/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.ObjectID;

import java.util.Collection;


public class ClockCacheTest extends LRUEvictionPolicyTest {
  public EvictionPolicy createNewCache(int size) {
    return new ClockEvictionPolicy(size);
  }
  
  public void testsClockMovement() throws Exception {
    int cacheSize = 10;
    EvictionPolicy slc = new ClockEvictionPolicy(cacheSize/2, 100);
    Cacheable[] cacheables = new Cacheable[cacheSize];
    for (int i = 0; i < cacheSize; i++) {
      cacheables[i] = new TestCacheable(new ObjectID(i));
      slc.add(cacheables[i]);
      cacheables[i].clearAccessed();
   //   assertFalse(evict);
    }
   
    for(int i = 0; i < cacheSize; i++) {
      Collection c = slc.getRemovalCandidates(2);
     
      TestCacheable cache1 = new TestCacheable(new ObjectID(i));
      System.out.println("collection : " + c);
      System.out.println("checking objectID: " + cache1);
      assertTrue(c.contains(cache1));
      i++;
      TestCacheable cache2 = new TestCacheable(new ObjectID(i));
       System.out.println("checking objectID: " + cache2);
      assertTrue(c.contains(cache2));
    }
    
    for(int i = 0; i < cacheSize; i++) {
      Collection c = slc.getRemovalCandidates(2);
      
      TestCacheable cache1 = new TestCacheable(new ObjectID(i));
      System.out.println("collection : " + c);
      System.out.println("checking objectID: " + cache1);
      assertTrue(c.contains(cache1));
      i++;
      TestCacheable cache2 = new TestCacheable(new ObjectID(i));
       System.out.println("checking objectID: " + cache2);
      assertTrue(c.contains(cache2));
    }
  }
  
}
