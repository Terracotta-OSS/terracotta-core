/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import com.tc.text.PrettyPrinter;

import java.util.Collection;
import java.util.Collections;

/**
 * @author steve
 */
public class NullCache implements EvictionPolicy {

  public synchronized boolean add(Cacheable obj) {
    return false;
  }
  
  public Collection getRemovalCandidates(int maxCount) {
    return Collections.EMPTY_LIST;
  }

  public synchronized void remove(Cacheable obj) {
    //nothing to remove. The cache is null
  }

  public synchronized void markReferenced(Cacheable obj) {
    //move the referenced object up in the lru
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.println("NULL CACHE");
  }

  public int getCacheCapacity() {
    return Integer.MAX_VALUE;
  }
}