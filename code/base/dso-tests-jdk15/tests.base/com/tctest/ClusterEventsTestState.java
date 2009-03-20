/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tcclient.cluster.DsoNode;

import java.util.HashMap;
import java.util.Map;

public class ClusterEventsTestState {
  private final Map<String, ClusterEventsTestListener> listeners = new HashMap<String, ClusterEventsTestListener>();

  public Map<String, ClusterEventsTestListener> getListeners() {
    return listeners;
  }

  public ClusterEventsTestListener getListenerForNode(final DsoNode node) {
    synchronized (listeners) {
      ClusterEventsTestListener listener = listeners.get(node.getId());
      if (null == listener) {
        listener = new ClusterEventsTestListener();
        listeners.put(node.getId(), listener);
      }

      return listener;
    }
  }
}
