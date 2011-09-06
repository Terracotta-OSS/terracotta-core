/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.restart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AppStateSnapshot {

  private final RestartTestApp[] apps;
  private final Set              inits   = new HashSet();
  private final Set              starts  = new HashSet();
  final Set                      holders = new HashSet();
  final Set                      waiters = new HashSet();
  private final Set              ends    = new HashSet();
  private final Map              sets    = new HashMap();

  public AppStateSnapshot(RestartTestApp[] apps) {
    this.apps = apps;
    sets.put(TestAppState.INIT, inits);
    sets.put(TestAppState.START, starts);
    sets.put(TestAppState.HOLDER, holders);
    sets.put(TestAppState.WAITER, waiters);
    sets.put(TestAppState.END, ends);
    takeSnapshot();
  }

  public synchronized RestartTestApp getHolder() {
    while (true) {
      takeSnapshot();
      if (allEnded()) return null;
      RestartTestApp[] h = getHolders();
      if (h.length == 1) return h[0];
    }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (Iterator i = sets.keySet().iterator(); i.hasNext();) {
      Object key = i.next();
      buf.append(key + ":" + ((Set) sets.get(key)).size() + " ");
    }
    return buf.toString();
  }

  private void clear() {
    for (Iterator i = sets.values().iterator(); i.hasNext();) {
      Set s = (Set) i.next();
      s.clear();
    }
  }

  public synchronized AppStateSnapshot takeSnapshot() {
    clear();
    for (int i = 0; i < apps.length; i++) {
      ((Set) sets.get(apps[i].getStateName())).add(apps[i]);
    }
    return this;
  }

  public synchronized RestartTestApp[] getHolders() {
    takeSnapshot();
    return toArray(holders);
  }

  public synchronized RestartTestApp[] getWaiters() {
    takeSnapshot();
    return toArray(waiters);
  }

  private RestartTestApp[] toArray(Set s) {
    RestartTestApp[] rv = new RestartTestApp[s.size()];
    return (RestartTestApp[]) s.toArray(rv);
  }

  public synchronized boolean allStarted() {
    takeSnapshot();
    return inits.size() == 0;
  }

  public boolean allWaiters() {
    takeSnapshot();
    return waiters.size() == apps.length;
  }

  public synchronized boolean allEnded() {
    takeSnapshot();
    return ends.size() == apps.length;
  }
}