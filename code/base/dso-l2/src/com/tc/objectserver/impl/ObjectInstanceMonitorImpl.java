/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

import java.util.HashMap;
import java.util.Map;

public class ObjectInstanceMonitorImpl implements ObjectInstanceMonitor, ObjectInstanceMonitorMBean {

  private final TObjectIntHashMap instanceCounts = new TObjectIntHashMap();

  public ObjectInstanceMonitorImpl() {
    //
  }

  public synchronized void instanceCreated(String type) {
    if (type == null) { throw new IllegalArgumentException(); }

    if (!instanceCounts.increment(type)) {
      instanceCounts.put(type, 1);
    }
  }

  public synchronized void instanceDestroyed(String type) {
    if (type == null) { throw new IllegalArgumentException(); }

    if (!instanceCounts.adjustValue(type, -1)) { throw new IllegalStateException("No count available for type " + type); }

    if (instanceCounts.get(type) <= 0) {
      instanceCounts.remove(type);
    }
  }

  public synchronized Map getInstanceCounts() {
    final Map rv = new HashMap();

    instanceCounts.forEachEntry(new TObjectIntProcedure() {
      public boolean execute(Object key, int value) {
        rv.put(key, new Integer(value));
        return true;
      }
    });

    return rv;
  }
}
