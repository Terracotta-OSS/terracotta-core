/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Accumulate all keys being managed by other nodes participating in this cache during 
 * this global eviction cycle with the goal of discovering what keys <strong>aren't</strong>
 * managed by any local cache.  These are the keys that need to be worked on by the global evictor.
 * The cycle is "started" by setting the global eviction flag then other nodes will report in as 
 * they do eviction until all nodes have been accounted for.  
 * 
 * Whatever keys aren't in the global key set are assumed to be orphans and are then undergo eviction
 * by the node assigned as the global evictor.
 */
class GlobalKeySet {
  // State - all guarded by "this" lock
  private boolean globalEvictionStarted;
  private Set keys = new HashSet();

  synchronized boolean inGlobalEviction() {
    return this.globalEvictionStarted;
  }
  
  synchronized void addLocalKeySet(Object[] keySet) {
    if(globalEvictionStarted) {
      if(keySet != null) {
        for(int i=0; i<keySet.length; i++) {
          keys.add(keySet[i]);
        }
      }
    }
  }
  
  synchronized void globalEvictionStart(Object[] localKeys) {
    this.keys.clear();
    globalEvictionStarted = true;
    addLocalKeySet(localKeys);
  }

  synchronized Collection globalEvictionEnd() {
    globalEvictionStarted = false;
    Set remoteKeys = new HashSet(keys);
    this.keys.clear();
    return remoteKeys;
  }

}
