/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectInstanceMonitorImpl implements ObjectInstanceMonitor, ObjectInstanceMonitorMBean {

  private final Map<String, Integer> instanceCounts = new HashMap<String, Integer>();

  public ObjectInstanceMonitorImpl() {
    //
  }

  @Override
  public synchronized void instanceCreated(String type) {
    if (type == null) { throw new IllegalArgumentException(); }

    Integer value = instanceCounts.get(type);
    if (value == null) {
      instanceCounts.put(type, 1);
    } else {
      instanceCounts.put(type, value + 1);
    }

  }

  @Override
  public synchronized void instanceDestroyed(String type) {
    if (type == null) { throw new IllegalArgumentException(); }
    Integer value = instanceCounts.get(type);
    if (value == null) {
      throw new IllegalStateException("No count available for type " + type);
    } else {
      instanceCounts.put(type, value - 1);
      if (instanceCounts.get(type) <= 0) {
        instanceCounts.remove(type);
      }
    }

  }

  @Override
  public synchronized Map getInstanceCounts() {
    final Map rv = new HashMap();
    for (Entry<String, Integer> entry : instanceCounts.entrySet()) {
      rv.put(entry.getKey(), entry.getValue());
    }

    return rv;
  }
}
