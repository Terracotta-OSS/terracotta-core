/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import junit.framework.TestCase;

public class ObjectIDSetPerfTest extends TestCase {

  private Random random = new Random();
  private static final int SET_SIZE = ObjectIDSet.MIN_JUMBO_SIZE - 1;
  
  public void setUp() {
    // warmup 
    hammerAdd(new ObjectIDSet(), 10000, SET_SIZE);
    hammerAdd(new JumboObjectIDSet(), 10000, 10000);
  }
  
  public void testAddPerformance() {
    int loops = 1000 * 1000;
    long elapsedSmall = hammerAdd(new ObjectIDSet(), loops, SET_SIZE);
    long elapsedJumbo = hammerAdd(new JumboObjectIDSet(), loops, loops*SET_SIZE);
    System.out.println("add: small = " + elapsedSmall + " ms, jumbo = " + elapsedJumbo + " ms");
    assertTrue(elapsedSmall <= elapsedJumbo);
  } 
  
  public void testRemovePerformance() {
    int loops = 1000 * 1000;
    long elapsedSmall = hammerRemove(new ObjectIDSet(), loops, SET_SIZE);
    long elapsedJumbo = hammerRemove(new JumboObjectIDSet(), loops, loops*SET_SIZE);
    System.out.println("add/remove: small = " + elapsedSmall + " ms, jumbo = " + elapsedJumbo + " ms");
    assertTrue(elapsedSmall <= elapsedJumbo);
  }
  
  public void testIterateAllPerformance() {
    int loops = 500 * 1000;
    long elapsedSmall = hammerIterate(new ObjectIDSet(), loops, SET_SIZE);
    long elapsedJumbo = hammerIterate(new JumboObjectIDSet(), loops, SET_SIZE);
    System.out.println("iterate all: small = " + elapsedSmall + " ms, jumbo = " + elapsedJumbo + " ms");
    assertTrue(elapsedSmall <= elapsedJumbo);
  }
  
  public void testFirstPerformance() {
    int loops = 10000 * 1000;
    long elapsedSmall = hammerFirst(new ObjectIDSet(), loops, SET_SIZE);
    long elapsedJumbo = hammerFirst(new JumboObjectIDSet(), loops, SET_SIZE);
    System.out.println("first: small = " + elapsedSmall + " ms, jumbo = " + elapsedJumbo + " ms");
    assertTrue(elapsedSmall <= elapsedJumbo);
  }

  private long hammerAdd(Set set, int loops, int range) {
    long begin = System.currentTimeMillis();
    
    for(int i=0; i<loops; i++) {
      set.add(new ObjectID(random.nextInt(range)));
    }
    
    return System.currentTimeMillis() - begin;
  }
  
  private long hammerRemove(Set set, int loops, int range) {    
    for(int i=0; i<loops; i++) {
      set.add(new ObjectID(random.nextInt(range)));
    }
    
    long begin = System.currentTimeMillis();
    for(int i=0; i<loops; i++) {
      set.remove(new ObjectID(random.nextInt(range)));
    }
    return System.currentTimeMillis() - begin;
  }

  private long hammerIterate(Set set, int loops, int range) {
    for(int i=0; i<range; i++) {
      set.add(new ObjectID(i));
    }
    
    long begin = System.currentTimeMillis();
    
    for(int i=0; i<loops; i++) {
      Iterator iter = set.iterator();
      while(iter.hasNext()) {
        iter.next();
      }
    }
    
    return System.currentTimeMillis() - begin;
  }
  
  private long hammerFirst(SortedSet set, int loops, int range) {
    for(int i=0; i<range; i++) {
      set.add(new ObjectID(i));
    }
    
    long begin = System.currentTimeMillis();
    
    for(int i=0; i<loops; i++) {
      Object foo = set.first();
      foo.hashCode();
    }
    
    return System.currentTimeMillis() - begin;
  }
 
}
