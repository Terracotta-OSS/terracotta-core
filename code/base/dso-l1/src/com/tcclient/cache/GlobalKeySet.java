/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class GlobalKeySet {
  private final Map    nodeIdsToKeysMap = new HashMap();
  private final String cacheName;
  private boolean      globalEvictionStarted;
  private Set          globalNodeIds = new HashSet();

  public GlobalKeySet(String cacheName) {
    this.cacheName = cacheName;
  }

  synchronized void waitForGlobalEviction() {
    while (!globalEvictionStarted) {
      try {
        wait();
      } catch (InterruptedException e) {
        // ignore the interrupt
      }
    }
  }

  synchronized void globalEvictionStart(String thisNodeId, Set globalNodeIds) {
    this.globalNodeIds.addAll(globalNodeIds);
    this.globalNodeIds.remove(thisNodeId);
    this.nodeIdsToKeysMap.clear();
    globalEvictionStarted = true;
    notifyAll();
    while (this.globalNodeIds.size() > 0) {
      try {
        wait();
      } catch (InterruptedException e) {
        // ignore the interrupt
      }
    }
  }

  synchronized void globalEvictionEnd() {
    globalEvictionStarted = false;
    globalNodeIds.clear();
  }

  synchronized void addLocalKeySet(String nodeId, Object[] keySet) {
    nodeIdsToKeysMap.put(nodeId, keySet);
    globalNodeIds.remove(nodeId);
    if (globalNodeIds.size() == 0) {
      notify();
    }
  }

  synchronized Collection allGlobalKeys() {
    return nodeIdsToKeysMap.values();
  }
}
